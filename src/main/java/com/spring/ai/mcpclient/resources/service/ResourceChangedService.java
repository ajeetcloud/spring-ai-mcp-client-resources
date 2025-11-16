package com.spring.ai.mcpclient.resources.service;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceChangedService implements McpSyncClientCustomizer {

    private final ResourceService resourceService;

    public ResourceChangedService(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public void customize(String name, McpClient.SyncSpec spec) {
        spec.resourcesChangeConsumer((List<McpSchema.Resource> resources) -> {
            System.out.println("Resource changed! Count: " + resources.size());
            resourceService.populateTextContent();
        });
    }


}


