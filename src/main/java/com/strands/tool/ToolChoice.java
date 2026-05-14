package com.strands.tool;

import java.util.Map;

/**
 * Configures how the model selects tools. Mirrors Python SDK's tool_choice parameter.
 */
public class ToolChoice {

    public enum Type {
        AUTO, ANY, TOOL, NONE
    }

    private final Type type;
    private final String toolName;

    private ToolChoice(Type type, String toolName) {
        this.type = type;
        this.toolName = toolName;
    }

    public static ToolChoice auto() {
        return new ToolChoice(Type.AUTO, null);
    }

    public static ToolChoice any() {
        return new ToolChoice(Type.ANY, null);
    }

    public static ToolChoice tool(String name) {
        return new ToolChoice(Type.TOOL, name);
    }

    public static ToolChoice none() {
        return new ToolChoice(Type.NONE, null);
    }

    public Type getType() {
        return type;
    }

    public String getToolName() {
        return toolName;
    }

    public Map<String, Object> toMap() {
        return switch (type) {
            case AUTO -> Map.of("auto", Map.of());
            case ANY -> Map.of("any", Map.of());
            case TOOL -> Map.of("tool", Map.of("name", toolName));
            case NONE -> Map.of();
        };
    }
}
