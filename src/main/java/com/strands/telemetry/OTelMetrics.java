package com.strands.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;

/**
 * OpenTelemetry metrics for Strands agents. Tracks token usage,
 * tool calls, event loop cycles, and latency.
 */
public class OTelMetrics {

    private static final String INSTRUMENTATION_SCOPE = "strands.agents";

    private final LongCounter tokenInputCounter;
    private final LongCounter tokenOutputCounter;
    private final LongCounter toolCallCounter;
    private final LongCounter toolErrorCounter;
    private final LongCounter eventLoopCycleCounter;
    private final LongHistogram modelLatency;
    private final LongHistogram toolLatency;

    public OTelMetrics(OpenTelemetry openTelemetry) {
        Meter meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);

        this.tokenInputCounter = meter.counterBuilder("gen_ai.client.token.usage")
                .setDescription("Input token count")
                .setUnit("tokens")
                .build();

        this.tokenOutputCounter = meter.counterBuilder("gen_ai.client.output_tokens")
                .setDescription("Output token count")
                .setUnit("tokens")
                .build();

        this.toolCallCounter = meter.counterBuilder("strands.tool.calls")
                .setDescription("Number of tool calls")
                .build();

        this.toolErrorCounter = meter.counterBuilder("strands.tool.errors")
                .setDescription("Number of tool call errors")
                .build();

        this.eventLoopCycleCounter = meter.counterBuilder("strands.event_loop.cycles")
                .setDescription("Number of event loop cycles")
                .build();

        this.modelLatency = meter.histogramBuilder("gen_ai.client.operation.duration")
                .setDescription("Model call latency")
                .setUnit("ms")
                .ofLongs()
                .build();

        this.toolLatency = meter.histogramBuilder("strands.tool.duration")
                .setDescription("Tool execution latency")
                .setUnit("ms")
                .ofLongs()
                .build();
    }

    public void recordTokenUsage(long inputTokens, long outputTokens, String modelId) {
        tokenInputCounter.add(inputTokens);
        tokenOutputCounter.add(outputTokens);
    }

    public void recordToolCall(String toolName, boolean success, long durationMs) {
        toolCallCounter.add(1);
        if (!success) {
            toolErrorCounter.add(1);
        }
        toolLatency.record(durationMs);
    }

    public void recordModelLatency(long durationMs, String modelId) {
        modelLatency.record(durationMs);
    }

    public void recordEventLoopCycle() {
        eventLoopCycleCounter.add(1);
    }
}
