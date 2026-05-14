package com.strands.tool;

import com.strands.types.ToolResult;
import com.strands.types.ToolSpec;
import com.strands.types.ToolUse;

public interface AgentTool {

    String getToolName();

    ToolSpec getToolSpec();

    ToolResult invoke(ToolUse toolUse, ToolContext context);
}
