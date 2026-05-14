package com.strands.hook.events;

import com.strands.hook.HookEvent;
import com.strands.multiagent.MultiAgentResult;

public class AfterMultiAgentInvocationEvent extends HookEvent {

    private final Object multiAgent;
    private final MultiAgentResult result;

    public AfterMultiAgentInvocationEvent(Object multiAgent, MultiAgentResult result) {
        this.multiAgent = multiAgent;
        this.result = result;
    }

    public Object getMultiAgent() {
        return multiAgent;
    }

    public MultiAgentResult getResult() {
        return result;
    }
}
