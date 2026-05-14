package com.strands.hook.events;

import com.strands.hook.HookEvent;

public class AgentInitializedEvent extends HookEvent {

    private final Object agent;

    public AgentInitializedEvent(Object agent) {
        this.agent = agent;
    }

    public Object getAgent() {
        return agent;
    }
}
