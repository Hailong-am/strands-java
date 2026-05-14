package com.strands.model;

import com.strands.types.*;
import com.strands.types.streaming.StreamEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StreamProcessor {

    @SuppressWarnings("unchecked")
    public StreamResult process(Iterator<StreamEvent> stream, StreamHandler handler) {
        Message.Role role = Message.Role.ASSISTANT;
        List<ContentBlock> contentBlocks = new ArrayList<>();
        StopReason stopReason = StopReason.END_TURN;
        Usage usage = new Usage();
        long latencyMs = 0;

        StringBuilder currentText = null;
        StringBuilder currentToolInput = null;
        ToolUse currentToolUse = null;

        while (stream.hasNext()) {
            StreamEvent event = stream.next();
            Map<String, Object> data = event.getData();

            switch (event.getType()) {
                case MESSAGE_START:
                    String roleStr = (String) data.get("role");
                    if ("user".equals(roleStr)) {
                        role = Message.Role.USER;
                    }
                    break;

                case CONTENT_BLOCK_START:
                    Map<String, Object> start = (Map<String, Object>) data.get("start");
                    if (start != null && start.containsKey("toolUse")) {
                        Map<String, Object> toolUseStart = (Map<String, Object>) start.get("toolUse");
                        currentToolUse = new ToolUse();
                        currentToolUse.setToolUseId((String) toolUseStart.get("toolUseId"));
                        currentToolUse.setName((String) toolUseStart.get("name"));
                        currentToolInput = new StringBuilder();
                        if (handler != null) {
                            handler.onToolUseStart(currentToolUse.getName(), currentToolUse.getToolUseId());
                        }
                    } else {
                        currentText = new StringBuilder();
                    }
                    break;

                case CONTENT_BLOCK_DELTA:
                    Map<String, Object> delta = (Map<String, Object>) data.get("delta");
                    if (delta != null) {
                        if (delta.containsKey("text")) {
                            String text = (String) delta.get("text");
                            if (currentText == null) {
                                currentText = new StringBuilder();
                            }
                            currentText.append(text);
                            if (handler != null) {
                                handler.onTextDelta(text);
                            }
                        } else if (delta.containsKey("toolUse")) {
                            Map<String, Object> toolUseDelta = (Map<String, Object>) delta.get("toolUse");
                            String input = (String) toolUseDelta.get("input");
                            if (input != null && currentToolInput != null) {
                                currentToolInput.append(input);
                            }
                            if (handler != null) {
                                handler.onToolUseDelta(input);
                            }
                        }
                    }
                    break;

                case CONTENT_BLOCK_STOP:
                    if (currentToolUse != null) {
                        String inputJson = currentToolInput != null ? currentToolInput.toString() : "{}";
                        currentToolUse.setInput(parseJsonInput(inputJson));
                        contentBlocks.add(ContentBlock.fromToolUse(currentToolUse));
                        currentToolUse = null;
                        currentToolInput = null;
                    } else if (currentText != null) {
                        String text = currentText.toString();
                        if (!text.isEmpty()) {
                            contentBlocks.add(ContentBlock.fromText(text));
                        }
                        currentText = null;
                    }
                    break;

                case MESSAGE_STOP:
                    if (currentText != null) {
                        String pendingText = currentText.toString();
                        if (!pendingText.isEmpty()) {
                            contentBlocks.add(ContentBlock.fromText(pendingText));
                        }
                        currentText = null;
                    }
                    String reason = (String) data.get("stopReason");
                    if (reason != null) {
                        stopReason = StopReason.fromValue(reason);
                    }
                    break;

                case METADATA:
                    Map<String, Object> usageData = (Map<String, Object>) data.get("usage");
                    if (usageData != null) {
                        long inputTokens = ((Number) usageData.get("inputTokens")).longValue();
                        long outputTokens = ((Number) usageData.get("outputTokens")).longValue();
                        usage = new Usage(inputTokens, outputTokens);
                    }
                    Map<String, Object> metrics = (Map<String, Object>) data.get("metrics");
                    if (metrics != null && metrics.containsKey("latencyMs")) {
                        latencyMs = ((Number) metrics.get("latencyMs")).longValue();
                    }
                    break;
            }
        }

        Message message = new Message(role, contentBlocks);
        if (handler != null) {
            handler.onComplete(message, stopReason);
        }
        return new StreamResult(message, stopReason, usage, latencyMs);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonInput(String json) {
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return Map.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }
}
