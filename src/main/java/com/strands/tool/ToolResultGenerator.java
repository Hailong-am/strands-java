package com.strands.tool;

import com.strands.types.Message;
import com.strands.types.ContentBlock;
import com.strands.types.ToolResult;
import com.strands.types.ToolUse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates missing tool result content when tool_use blocks in assistant messages
 * have no corresponding tool_result in the subsequent user message. This prevents
 * model API errors that occur when tool results are missing from conversation history.
 */
public class ToolResultGenerator {

    public static List<ToolResult> generateMissingToolResults(List<Message> messages) {
        List<ToolResult> missing = new ArrayList<>();

        for (int i = 0; i < messages.size() - 1; i++) {
            Message msg = messages.get(i);
            if (msg.getRole() != Message.Role.ASSISTANT || !msg.hasToolUse()) {
                continue;
            }

            List<ToolUse> toolUses = msg.getToolUses();
            Set<String> answeredIds = getToolResultIds(messages, i + 1);

            for (ToolUse toolUse : toolUses) {
                if (!answeredIds.contains(toolUse.getToolUseId())) {
                    missing.add(ToolResult.error(toolUse.getToolUseId(),
                            "Tool result not available (conversation was interrupted or tool was cancelled)"));
                }
            }
        }

        if (!messages.isEmpty()) {
            Message last = messages.get(messages.size() - 1);
            if (last.getRole() == Message.Role.ASSISTANT && last.hasToolUse()) {
                for (ToolUse toolUse : last.getToolUses()) {
                    missing.add(ToolResult.error(toolUse.getToolUseId(),
                            "Tool result not available (conversation was interrupted or tool was cancelled)"));
                }
            }
        }

        return missing;
    }

    private static Set<String> getToolResultIds(List<Message> messages, int startIndex) {
        Set<String> ids = new java.util.HashSet<>();
        for (int i = startIndex; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (msg.getRole() == Message.Role.USER) {
                for (ContentBlock block : msg.getContent()) {
                    if (block.isToolResult() && block.getToolResult() != null) {
                        ids.add(block.getToolResult().getToolUseId());
                    }
                }
                break;
            }
        }
        return ids;
    }
}
