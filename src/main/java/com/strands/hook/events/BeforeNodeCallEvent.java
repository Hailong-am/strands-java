package com.strands.hook.events;

import com.strands.hook.HookEvent;

public class BeforeNodeCallEvent extends HookEvent {

    private final String nodeName;
    private final Object agent;
    private boolean cancelled;
    private boolean interrupted;

    public BeforeNodeCallEvent(String nodeName, Object agent) {
        this.nodeName = nodeName;
        this.agent = agent;
    }

    public String getNodeName() {
        return nodeName;
    }

    public Object getAgent() {
        return agent;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}
