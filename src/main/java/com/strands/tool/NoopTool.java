package com.strands.tool;

import com.strands.types.ToolResult;
import com.strands.types.ToolSpec;
import com.strands.types.ToolUse;

import java.util.Map;

/**
 * A no-operation tool that always returns an empty success result.
 * Used internally when the model references a tool that doesn't exist,
 * preventing the agent loop from failing on unknown tool calls.
 */
public class NoopTool implements AgentTool {

    public static final NoopTool INSTANCE = new NoopTool();
    public static final String TOOL_NAME = "noop_tool";

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public ToolSpec getToolSpec() {
        return new ToolSpec(TOOL_NAME, "No-op tool placeholder", Map.of(
                "type", "object",
                "properties", Map.of()
        ));
    }

    @Override
    public ToolResult invoke(ToolUse toolUse, ToolContext context) {
        return ToolResult.success(toolUse.getToolUseId(), "");
    }
}
