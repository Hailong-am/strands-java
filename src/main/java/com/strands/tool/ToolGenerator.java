package com.strands.tool;

import com.strands.types.ToolResult;
import com.strands.types.ToolUse;

import java.util.Iterator;

/**
 * A tool that yields intermediate events during execution, enabling streaming
 * of partial results back to the caller. Similar to Python generator tools.
 *
 * Implementations return an Iterator of ToolGeneratorEvent, where each event
 * is either an intermediate result or the final result.
 */
public interface ToolGenerator extends AgentTool {

    Iterator<ToolGeneratorEvent> invokeStreaming(ToolUse toolUse, ToolContext context);

    @Override
    default ToolResult invoke(ToolUse toolUse, ToolContext context) {
        Iterator<ToolGeneratorEvent> events = invokeStreaming(toolUse, context);
        ToolResult lastResult = null;
        while (events.hasNext()) {
            ToolGeneratorEvent event = events.next();
            if (event.isFinal()) {
                lastResult = event.getResult();
            }
        }
        return lastResult != null ? lastResult : ToolResult.success(toolUse.getToolUseId(), "");
    }

    record ToolGeneratorEvent(ToolResult result, boolean isFinal, String intermediateText) {
        public static ToolGeneratorEvent intermediate(String text) {
            return new ToolGeneratorEvent(null, false, text);
        }

        public static ToolGeneratorEvent complete(ToolResult result) {
            return new ToolGeneratorEvent(result, true, null);
        }

        public ToolResult getResult() { return result; }
    }
}
