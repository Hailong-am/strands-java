package com.strands.tool;

import com.strands.types.ToolUse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ToolContext {

    private final ToolUse toolUse;
    private final Map<String, Object> invocationState;

    public ToolContext(ToolUse toolUse, Map<String, Object> invocationState) {
        this.toolUse = toolUse;
        this.invocationState = invocationState != null ? invocationState : new ConcurrentHashMap<>();
    }

    public ToolUse getToolUse() {
        return toolUse;
    }

    public Map<String, Object> getInvocationState() {
        return invocationState;
    }

    @SuppressWarnings("unchecked")
    public <T> T getState(String key, Class<T> type) {
        return (T) invocationState.get(key);
    }

    public void setState(String key, Object value) {
        invocationState.put(key, value);
    }
}
