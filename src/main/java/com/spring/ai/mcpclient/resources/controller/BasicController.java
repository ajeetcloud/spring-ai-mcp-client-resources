package com.spring.ai.mcpclient.resources.controller;

import com.spring.ai.mcpclient.resources.service.ResourceConsumerService;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class BasicController {

    @Autowired
    private ResourceConsumerService consumerService;

    private final ChatClient chatClient;

    private List<McpSyncClient> mcpSyncClients;

    public BasicController(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.mcpSyncClients = mcpSyncClients;
        this.chatClient = chatClientBuilder.build();

        for (McpSyncClient client : mcpSyncClients) {
            try {
                // List all available resources
                McpSchema.ListResourcesResult resourcesList = client.listResources();

                System.out.println("Available resources: " + resourcesList.resources().size());

                for (McpSchema.Resource resource : resourcesList.resources()) {
                    System.out.println("Resource URI: " + resource.uri());
                    System.out.println("Resource Name: " + resource.name());
                    System.out.println("MIME Type: " + resource.mimeType());

                    McpSchema.ReadResourceResult result = client.readResource(
                            new McpSchema.ReadResourceRequest(resource.uri())
                    );

                    for (var content : result.contents()) {
                        if (content instanceof McpSchema.TextResourceContents textContent) {
                            System.out.println("Text content: " + textContent.text());
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @GetMapping("/chat")
    public String chat() {
        String userQuestion = "How many years of experience does Ajeet has?";
        if (mcpSyncClients.isEmpty()) {
            return "No MCP clients available";
        }
        McpSyncClient client = mcpSyncClients.get(0);
        try {
            McpSchema.ListResourcesResult resourcesList = client.listResources();
            List<McpSchema.Resource> resources = resourcesList.resources();
            String combinedContext = buildCombinedContext(client, resources);

            String answer = chatClient.prompt()
                    .user(u -> u
                            .text("""
                                You have access to the following documents. Answer the user's question based on the information provided.
                                
                                DOCUMENTS:
                                {context}
                                
                                USER QUESTION: {question}
                                
                                Provide a clear, well-structured answer. If the answer requires information from multiple documents, reference them by name.
                                """)
                            .param("context", combinedContext)
                            .param("question", userQuestion)
                    )
                    .call()
                    .content();

            return answer;
        } catch(Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/ask-specific")
    public Map<String, Object> chatWithSpecificResources(@RequestBody Map<String, Object> request) {
        String userQuestion = (String) request.get("question");
        List<String> documentNames = (List<String>) request.get("documents");

        if (mcpSyncClients.isEmpty()) {
            return Map.of("error", "No MCP clients available");
        }

        McpSyncClient client = mcpSyncClients.get(0);

        try {
            // List all resources
            McpSchema.ListResourcesResult resourcesList = client.listResources();

            // Filter only the requested documents
            List<McpSchema.Resource> requestedResources = resourcesList.resources().stream()
                    .filter(r -> documentNames.contains(r.name()))
                    .collect(Collectors.toList());

            if (requestedResources.isEmpty()) {
                return Map.of("error", "None of the requested documents found");
            }

            System.out.println("📚 Loading " + requestedResources.size() + " specific resources...");

            // Build context from selected resources
            String combinedContext = buildCombinedContext(client, requestedResources);

            // Single AI call
            String answer = chatClient.prompt()
                    .user(u -> u
                            .text("""
                                You have access to the following documents. Answer the user's question based on the information provided.
                                
                                DOCUMENTS:
                                {context}
                                
                                USER QUESTION: {question}
                                
                                Provide a clear answer based only on the documents provided.
                                """)
                            .param("context", combinedContext)
                            .param("question", userQuestion)
                    )
                    .call()
                    .content();

            return Map.of(
                    "question", userQuestion,
                    "answer", answer,
                    "documentsUsed", requestedResources.stream()
                            .map(McpSchema.Resource::name)
                            .collect(Collectors.toList())
            );

        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }


    private String buildCombinedContext(McpSyncClient client, List<McpSchema.Resource> resources) {
        StringBuilder contextBuilder = new StringBuilder();

        for (McpSchema.Resource resource : resources) {
            try {
                // Read the resource
                McpSchema.ReadResourceResult result = client.readResource(
                        new McpSchema.ReadResourceRequest(resource.uri())
                );

                // Extract text content
                String fileContent = result.contents().stream()
                        .filter(c -> c instanceof McpSchema.TextResourceContents)
                        .map(c -> ((McpSchema.TextResourceContents) c).text())
                        .findFirst()
                        .orElse("");

                if (!fileContent.isEmpty()) {
                    // Add document separator with clear labeling
                    contextBuilder.append("\n\n")
                            .append("=".repeat(60))
                            .append("\n")
                            .append("DOCUMENT: ")
                            .append(resource.name())
                            .append("\n")
                            .append("=".repeat(60))
                            .append("\n")
                            .append(fileContent);

                    System.out.println("   ✓ Loaded: " + resource.name());
                }

            } catch (Exception e) {
                System.err.println("   ✗ Failed to read: " + resource.name() + " - " + e.getMessage());
            }
        }

        return contextBuilder.toString();
    }

    @GetMapping("/documents")
    public Map<String, Object> listAvailableDocuments() {
        if (mcpSyncClients.isEmpty()) {
            return Map.of("error", "No MCP clients available");
        }

        try {
            McpSyncClient client = mcpSyncClients.get(0);
            McpSchema.ListResourcesResult resourcesList = client.listResources();

            List<Map<String, String>> documents = resourcesList.resources().stream()
                    .map(r -> Map.of(
                            "name", r.name(),
                            "uri", r.uri(),
                            "description", r.description() != null ? r.description() : ""
                    ))
                    .collect(Collectors.toList());

            return Map.of(
                    "total", documents.size(),
                    "documents", documents
            );

        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    public String chatWithFileContext(String userQuestion, String fileUri) {
        McpSyncClient client = mcpSyncClients.get(0);
        McpSchema.ReadResourceResult result = client.readResource(new McpSchema.ReadResourceRequest(fileUri));

        String fileContent = result.contents().stream()
                .filter(c -> c instanceof McpSchema.TextResourceContents)
                .map(c -> ((McpSchema.TextResourceContents) c).text())
                .findFirst()
                .orElse("");

        // Use in AI prompt
        return chatClient.prompt()
                .user(u -> u
                        .text("Based on this file content: {content}\n\nAnswer this question: {question}")
                        .param("content", fileContent)
                        .param("question", userQuestion)
                )
                .call()
                .content();
    }
}
