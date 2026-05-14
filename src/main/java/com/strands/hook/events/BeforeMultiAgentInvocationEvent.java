package com.strands.hook.events;

import com.strands.hook.HookEvent;

import java.util.Map;

public class BeforeMultiAgentInvocationEvent extends HookEvent {

    private final Object multiAgent;
    private final Map<String, Object> inputs;
    private boolean cancelled;

    public BeforeMultiAgentInvocationEvent(Object multiAgent, Map<String, Object> inputs) {
        this.multiAgent = multiAgent;
        this.inputs = inputs;
    }

    public Object getMultiAgent() {
        return multiAgent;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
