package com.strands.tool.mcp;

import java.util.Map;

public class MCPToolDefinition {

    private final String name;
    private final String description;
    private final Map<String, Object> inputSchema;
    private final Map<String, Object> outputSchema;

    public MCPToolDefinition(String name, String description, Map<String, Object> inputSchema) {
        this(name, description, inputSchema, null);
    }

    public MCPToolDefinition(String name, String description, Map<String, Object> inputSchema, Map<String, Object> outputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    public Map<String, Object> getOutputSchema() {
        return outputSchema;
    }
}
