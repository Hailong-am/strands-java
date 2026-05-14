package com.strands.handler;

import java.util.Map;

public class PrintingCallbackHandler implements CallbackHandler {

    private final boolean verboseToolUse;
    private int toolCount;

    public PrintingCallbackHandler() {
        this(true);
    }

    public PrintingCallbackHandler(boolean verboseToolUse) {
        this.verboseToolUse = verboseToolUse;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(Map<String, Object> kwargs) {
        if (kwargs.containsKey("reasoningText")) {
            System.out.print(kwargs.get("reasoningText"));
            return;
        }

        if (kwargs.containsKey("data")) {
            System.out.print(kwargs.get("data"));
            if (Boolean.TRUE.equals(kwargs.get("complete"))) {
                System.out.println();
            }
            return;
        }

        if (verboseToolUse && kwargs.containsKey("event")) {
            Map<String, Object> event = (Map<String, Object>) kwargs.get("event");
            if (event.containsKey("contentBlockStart")) {
                Map<String, Object> blockStart = (Map<String, Object>) event.get("contentBlockStart");
                Map<String, Object> start = (Map<String, Object>) blockStart.get("start");
                if (start != null && start.containsKey("toolUse")) {
                    Map<String, Object> toolUse = (Map<String, Object>) start.get("toolUse");
                    toolCount++;
                    System.out.printf("%nTool #%d: %s%n", toolCount, toolUse.get("name"));
                }
            }
        }
    }

    public int getToolCount() {
        return toolCount;
    }
}
