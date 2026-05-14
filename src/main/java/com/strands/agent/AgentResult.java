package com.strands.agent;

import com.strands.types.Message;
import com.strands.types.Metrics;
import com.strands.types.StopReason;

public class AgentResult {

    private final StopReason stopReason;
    private final Message message;
    private final Metrics metrics;
    private final AgentState state;

    public AgentResult(StopReason stopReason, Message message, Metrics metrics, AgentState state) {
        this.stopReason = stopReason;
        this.message = message;
        this.metrics = metrics;
        this.state = state;
    }

    public StopReason getStopReason() {
        return stopReason;
    }

    public Message getMessage() {
        return message;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public AgentState getState() {
        return state;
    }

    @Override
    public String toString() {
        if (message == null) return "";
        return message.getTextContent();
    }
}
