package com.strands.types;

import lombok.Getter;

@Getter
public class Metrics {

    private long latencyMs;
    private int cycleCount;
    private final Usage accumulatedUsage = new Usage();

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public void incrementCycleCount() {
        this.cycleCount++;
    }

    public void accumulateUsage(Usage usage) {
        this.accumulatedUsage.accumulate(usage);
    }
}
