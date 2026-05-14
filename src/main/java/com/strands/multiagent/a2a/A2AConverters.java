package com.strands.multiagent.a2a;

import com.strands.types.*;

import java.util.*;

/**
 * Converts between internal Strands message format and A2A protocol format.
 * The A2A (Agent-to-Agent) protocol uses a standard JSON-based message format
 * for inter-agent communication.
 */
public class A2AConverters {

    public static Map<String, Object> toA2AMessage(Message message) {
        Map<String, Object> a2aMsg = new LinkedHashMap<>();
        a2aMsg.put("role", message.getRole().getValue());

        List<Map<String, Object>> parts = new ArrayList<>();
        for (ContentBlock block : message.getContent()) {
            if (block.isText()) {
                parts.add(Map.of("type", "text", "text", block.getText()));
            } else if (block.isToolUse()) {
                ToolUse tu = block.getToolUse();
                Map<String, Object> part = new LinkedHashMap<>();
                part.put("type", "function_call");
                part.put("id", tu.getToolUseId());
                part.put("name", tu.getName());
                part.put("arguments", tu.getInput() != null ? tu.getInput() : Map.of());
                parts.add(part);
            } else if (block.isToolResult()) {
                ToolResult tr = block.getToolResult();
                Map<String, Object> part = new LinkedHashMap<>();
                part.put("type", "function_result");
                part.put("id", tr.getToolUseId());
                part.put("status", tr.getStatus().getValue());
                StringBuilder content = new StringBuilder();
                if (tr.getContent() != null) {
                    for (ToolResultContent c : tr.getContent()) {
                        if (c.getText() != null) content.append(c.getText());
                    }
                }
                part.put("content", content.toString());
                parts.add(part);
            }
        }
        a2aMsg.put("parts", parts);
        return a2aMsg;
    }

    @SuppressWarnings("unchecked")
    public static Message fromA2AMessage(Map<String, Object> a2aMsg) {
        String role = (String) a2aMsg.get("role");
        Message.Role msgRole = "assistant".equals(role) ? Message.Role.ASSISTANT : Message.Role.USER;

        List<ContentBlock> blocks = new ArrayList<>();
        List<Map<String, Object>> parts = (List<Map<String, Object>>) a2aMsg.get("parts");

        if (parts != null) {
            for (Map<String, Object> part : parts) {
                String type = (String) part.get("type");
                switch (type) {
                    case "text" -> blocks.add(ContentBlock.fromText((String) part.get("text")));
                    case "function_call" -> {
                        ToolUse tu = new ToolUse();
                        tu.setToolUseId((String) part.get("id"));
                        tu.setName((String) part.get("name"));
                        tu.setInput((Map<String, Object>) part.getOrDefault("arguments", Map.of()));
                        blocks.add(ContentBlock.fromToolUse(tu));
                    }
                    case "function_result" -> {
                        String content = (String) part.getOrDefault("content", "");
                        String statusStr = (String) part.getOrDefault("status", "success");
                        ToolResult.Status status = "error".equals(statusStr)
                                ? ToolResult.Status.ERROR : ToolResult.Status.SUCCESS;
                        ToolResult tr = new ToolResult(
                                (String) part.get("id"), status,
                                List.of(ToolResultContent.fromText(content)));
                        blocks.add(ContentBlock.fromToolResult(tr));
                    }
                    default -> {}
                }
            }
        }

        return new Message(msgRole, blocks);
    }

    public static List<Map<String, Object>> toA2AConversation(List<Message> messages) {
        List<Map<String, Object>> conversation = new ArrayList<>();
        for (Message msg : messages) {
            conversation.add(toA2AMessage(msg));
        }
        return conversation;
    }

    public static List<Message> fromA2AConversation(List<Map<String, Object>> conversation) {
        List<Message> messages = new ArrayList<>();
        for (Map<String, Object> a2aMsg : conversation) {
            messages.add(fromA2AMessage(a2aMsg));
        }
        return messages;
    }
}
