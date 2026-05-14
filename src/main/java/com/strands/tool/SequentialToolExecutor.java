package com.strands.tool;

import com.strands.types.ToolResult;
import com.strands.types.ToolResultContent;
import com.strands.types.ToolUse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SequentialToolExecutor implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(SequentialToolExecutor.class);

    @Override
    public List<ToolResult> execute(List<ToolUse> toolUses, ToolRegistry registry, Map<String, Object> invocationState) {
        List<ToolResult> results = new ArrayList<>();
        for (ToolUse toolUse : toolUses) {
            AgentTool tool = registry.get(toolUse.getName());
            if (tool == null) {
                log.warn("Tool not found: {}", toolUse.getName());
                results.add(new ToolResult(toolUse.getToolUseId(), ToolResult.Status.ERROR,
                        List.of(ToolResultContent.fromText("Tool not found: " + toolUse.getName()))));
                continue;
            }

            ToolContext context = new ToolContext(toolUse, invocationState);
            try {
                results.add(tool.invoke(toolUse, context));
            } catch (Exception e) {
                log.error("Tool execution failed: {}", toolUse.getName(), e);
                results.add(new ToolResult(toolUse.getToolUseId(), ToolResult.Status.ERROR,
                        List.of(ToolResultContent.fromText("Tool execution error: " + e.getMessage()))));
            }
        }
        return results;
    }
}
