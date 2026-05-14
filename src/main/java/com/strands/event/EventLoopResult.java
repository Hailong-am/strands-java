package com.strands.event;

import com.strands.types.Message;
import com.strands.types.Metrics;
import com.strands.types.StopReason;

public class EventLoopResult {

    private final Message message;
    private final StopReason stopReason;
    private final Metrics metrics;

    public EventLoopResult(Message message, StopReason stopReason, Metrics metrics) {
        this.message = message;
        this.stopReason = stopReason;
        this.metrics = metrics;
    }

    public Message getMessage() {
        return message;
    }

    public StopReason getStopReason() {
        return stopReason;
    }

    public Metrics getMetrics() {
        return metrics;
    }
}
