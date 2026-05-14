package com.strands.hook.events;

import com.strands.hook.HookEvent;

public class MultiAgentInitializedEvent extends HookEvent {

    private final Object multiAgent;

    public MultiAgentInitializedEvent(Object multiAgent) {
        this.multiAgent = multiAgent;
    }

    public Object getMultiAgent() {
        return multiAgent;
    }
}
