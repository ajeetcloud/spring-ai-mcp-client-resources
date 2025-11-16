package com.spring.ai.mcpclient.resources.service;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {

    private final ApplicationContext applicationContext;

    private String textContent;

    public ResourceService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void populateTextContent() {

        List<McpSyncClient> clients = applicationContext.getBean("mcpSyncClients", List.class);
        McpSyncClient client = clients.getFirst();

        McpSchema.ListResourcesResult resourcesList = client.listResources();
        McpSchema.Resource resource = resourcesList.resources().getFirst();
        McpSchema.ReadResourceResult result = client.readResource(new McpSchema.ReadResourceRequest(resource.uri()));

        this.textContent = result.contents().stream()
                .filter(c -> c instanceof McpSchema.TextResourceContents)
                .map(c -> ((McpSchema.TextResourceContents) c).text())
                .findFirst()
                .orElse("");
    }

    public String getTextContent() {
        return this.textContent;
    }

}
