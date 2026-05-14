package com.strands.types.events;

import com.strands.types.Message;
import com.strands.types.Metrics;
import com.strands.types.StopReason;

import java.util.Map;

public class EventLoopStopEvent extends TypedEvent {

    private final StopReason stopReason;
    private final Message message;
    private final Metrics metrics;

    public EventLoopStopEvent(StopReason stopReason, Message message, Metrics metrics) {
        super("event_loop_stop", Map.of());
        this.stopReason = stopReason;
        this.message = message;
        this.metrics = metrics;
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

    @Override
    public boolean isCallbackEvent() {
        return false;
    }
}
