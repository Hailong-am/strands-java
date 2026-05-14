package com.strands.telemetry;

import com.strands.types.Usage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventLoopMetrics {

    private int cycleCount;
    private final Map<String, ToolMetrics> toolMetrics = new ConcurrentHashMap<>();
    private final List<Double> cycleDurations = new ArrayList<>();
    private final List<AgentInvocation> agentInvocations = new ArrayList<>();
    private final Usage accumulatedUsage = new Usage();

    public Cycle startCycle() {
        return new Cycle(System.nanoTime());
    }

    public void endCycle(Cycle cycle) {
        cycleCount++;
        double durationSec = (System.nanoTime() - cycle.startNanos) / 1_000_000_000.0;
        cycleDurations.add(durationSec);
    }

    public void addToolUsage(String toolName, double durationSec, boolean success) {
        toolMetrics.computeIfAbsent(toolName, k -> new ToolMetrics(toolName))
                .addCall(durationSec, success);
    }

    public void updateUsage(Usage usage) {
        accumulatedUsage.accumulate(usage);
        AgentInvocation current = getLatestAgentInvocation();
        if (current != null) {
            current.getUsage().accumulate(usage);
        }
    }

    public void resetForNewInvocation() {
        agentInvocations.add(new AgentInvocation());
    }

    public int getCycleCount() {
        return cycleCount;
    }

    public Map<String, ToolMetrics> getToolMetrics() {
        return toolMetrics;
    }

    public List<Double> getCycleDurations() {
        return cycleDurations;
    }

    public Usage getAccumulatedUsage() {
        return accumulatedUsage;
    }

    public AgentInvocation getLatestAgentInvocation() {
        return agentInvocations.isEmpty() ? null : agentInvocations.get(agentInvocations.size() - 1);
    }

    public record Cycle(long startNanos) {
    }

    public static class ToolMetrics {
        private final String toolName;
        private int callCount;
        private int successCount;
        private int errorCount;
        private double totalTime;

        public ToolMetrics(String toolName) {
            this.toolName = toolName;
        }

        public void addCall(double durationSec, boolean success) {
            callCount++;
            totalTime += durationSec;
            if (success) successCount++;
            else errorCount++;
        }

        public String getToolName() { return toolName; }
        public int getCallCount() { return callCount; }
        public int getSuccessCount() { return successCount; }
        public int getErrorCount() { return errorCount; }
        public double getTotalTime() { return totalTime; }
    }

    public static class AgentInvocation {
        private final Usage usage = new Usage();
        private final List<CycleMetric> cycles = new ArrayList<>();

        public Usage getUsage() { return usage; }
        public List<CycleMetric> getCycles() { return cycles; }
    }

    public static class CycleMetric {
        private final String cycleId;
        private final Usage usage;

        public CycleMetric(String cycleId, Usage usage) {
            this.cycleId = cycleId;
            this.usage = usage;
        }

        public String getCycleId() { return cycleId; }
        public Usage getUsage() { return usage; }
    }
}
