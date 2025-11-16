package com.spring.ai.mcpclient.resources.controller;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class BasicController {

    private final ChatClient chatClient;

    private List<McpSyncClient> mcpSyncClients;

    private String textContent;

    @PostConstruct
    public void init() {
        populateTextContent();
    }

    public BasicController(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.mcpSyncClients = mcpSyncClients;
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/documents")
    public Map<String, Object> listAvailableDocuments() {
        if (mcpSyncClients.isEmpty()) {
            return Map.of("error", "No MCP clients available");
        }
        try {
            McpSyncClient client = mcpSyncClients.getFirst();
            McpSchema.ListResourcesResult resourcesList = client.listResources();

            List<Map<String, String>> documents = resourcesList.resources().stream()
                    .map(r -> Map.of(
                            "name", r.name(),
                            "uri", r.uri(),
                            "description", r.description() != null ? r.description() : ""
                    ))
                    .toList();

            return Map.of(
                    "total", documents.size(),
                    "documents", documents
            );

        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String query) {
        if (mcpSyncClients.isEmpty()) {
            return "No MCP clients available";
        }
        return chatClient.prompt()
                .user(u -> u
                            .text("Based on this file content: {content}\n\nAnswer this question: {question}")
                            .param("content", textContent)
                            .param("question", query)
                )
                .call()
                .content();
    }

    private void populateTextContent() {

        McpSyncClient client = mcpSyncClients.getFirst();
        McpSchema.ListResourcesResult resourcesList = client.listResources();
        McpSchema.Resource resource = resourcesList.resources().getFirst();
        McpSchema.ReadResourceResult result = client.readResource(new McpSchema.ReadResourceRequest(resource.uri()));

        textContent = result.contents().stream()
                .filter(c -> c instanceof McpSchema.TextResourceContents)
                .map(c -> ((McpSchema.TextResourceContents) c).text())
                .findFirst()
                .orElse("");
    }

}
