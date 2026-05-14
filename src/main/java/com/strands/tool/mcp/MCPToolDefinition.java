package com.strands.tool.mcp;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class MCPToolDefinition {

    private final String name;
    private final String description;
    private final Map<String, Object> inputSchema;
    private final Map<String, Object> outputSchema;

    public MCPToolDefinition(String name, String description, Map<String, Object> inputSchema) {
        this(name, description, inputSchema, null);
    }
}
