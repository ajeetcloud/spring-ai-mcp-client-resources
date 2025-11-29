package com.spring.ai.mcpclient.resources.controller;

import com.spring.ai.mcpclient.resources.service.ResourceService;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class BasicController {

    private final ChatClient chatClient;

    private final List<McpSyncClient> mcpSyncClients;

    @Autowired
    private ResourceService resourceService;

    @PostConstruct
    public void init() {
        resourceService.populateTextContent();
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

        return chatClient.prompt()
                .user(u -> u
                            .text("Based on this file content: {content}\n\nAnswer this question: {question}")
                            .param("content", this.resourceService.getTextContent())
                            .param("question", query)
                )
                .call()
                .content();
    }

}
