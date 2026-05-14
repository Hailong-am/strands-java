package com.strands.model;

import com.strands.types.Message;
import com.strands.types.StopReason;
import com.strands.types.Usage;

public class StreamResult {

    private final Message message;
    private final StopReason stopReason;
    private final Usage usage;
    private final long latencyMs;

    public StreamResult(Message message, StopReason stopReason, Usage usage, long latencyMs) {
        this.message = message;
        this.stopReason = stopReason;
        this.usage = usage;
        this.latencyMs = latencyMs;
    }

    public Message getMessage() {
        return message;
    }

    public StopReason getStopReason() {
        return stopReason;
    }

    public Usage getUsage() {
        return usage;
    }

    public long getLatencyMs() {
        return latencyMs;
    }
}
