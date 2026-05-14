package com.strands.types.events;

import java.util.Map;

public class ToolStreamEvent extends TypedEvent {

    private final String toolName;
    private final String toolUseId;

    public ToolStreamEvent(String toolName, String toolUseId, Map<String, Object> data) {
        super("tool_stream", data);
        this.toolName = toolName;
        this.toolUseId = toolUseId;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolUseId() {
        return toolUseId;
    }
}
