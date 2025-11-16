package com.spring.ai.mcpclient.resources.service;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceConsumerService {

    @Autowired
    private List<McpSyncClient> mcpSyncClients;

    public void getData() {
        for (McpSyncClient client : mcpSyncClients) {
            McpSchema.ListResourcesResult resourcesList = client.listResources();

            System.out.println("Available resources: " + resourcesList.resources().size());

        }
    }


}
