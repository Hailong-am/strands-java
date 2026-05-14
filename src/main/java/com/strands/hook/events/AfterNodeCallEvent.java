package com.strands.hook.events;

import com.strands.hook.HookEvent;
import com.strands.multiagent.NodeResult;

public class AfterNodeCallEvent extends HookEvent {

    private final String nodeName;
    private final NodeResult result;

    public AfterNodeCallEvent(String nodeName, NodeResult result) {
        this.nodeName = nodeName;
        this.result = result;
    }

    public String getNodeName() {
        return nodeName;
    }

    public NodeResult getResult() {
        return result;
    }
}
