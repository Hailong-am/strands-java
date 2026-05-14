package com.strands.types;

public class Metrics {

    private long latencyMs;
    private int cycleCount;
    private Usage accumulatedUsage;

    public Metrics() {
        this.accumulatedUsage = new Usage();
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public int getCycleCount() {
        return cycleCount;
    }

    public void incrementCycleCount() {
        this.cycleCount++;
    }

    public Usage getAccumulatedUsage() {
        return accumulatedUsage;
    }

    public void accumulateUsage(Usage usage) {
        this.accumulatedUsage.accumulate(usage);
    }
}
