package com.spring.ai.mcpclient.resources.controller;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BasicController {

    private final ChatClient chatClient;

    public BasicController(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.chatClient = chatClientBuilder
                .defaultSystem("Please prioritise context information for answering questions. Give short, concise and to the point answers.")
                .build();

        for (McpSyncClient client : mcpSyncClients) {
            try {
                // List all available resources
                McpSchema.ListResourcesResult resourcesList = client.listResources();

                System.out.println("Available resources: " + resourcesList.resources().size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
