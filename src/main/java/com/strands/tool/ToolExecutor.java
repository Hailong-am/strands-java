package com.strands.tool;

import com.strands.types.ToolResult;
import com.strands.types.ToolUse;

import java.util.List;
import java.util.Map;

public interface ToolExecutor {

    List<ToolResult> execute(List<ToolUse> toolUses, ToolRegistry registry, Map<String, Object> invocationState);
}
