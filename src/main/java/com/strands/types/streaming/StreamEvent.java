package com.strands.types.streaming;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamEvent {

    public enum Type {
        MESSAGE_START,
        CONTENT_BLOCK_START,
        CONTENT_BLOCK_DELTA,
        CONTENT_BLOCK_STOP,
        MESSAGE_STOP,
        METADATA
    }

    private Type type;
    private Map<String, Object> data;

    public StreamEvent() {
    }

    public StreamEvent(Type type, Map<String, Object> data) {
        this.type = type;
        this.data = data;
    }

    public static StreamEvent messageStart(String role) {
        return new StreamEvent(Type.MESSAGE_START, Map.of("role", role));
    }

    public static StreamEvent contentBlockStart(int index, Map<String, Object> start) {
        return new StreamEvent(Type.CONTENT_BLOCK_START,
                Map.of("contentBlockIndex", index, "start", start));
    }

    public static StreamEvent contentBlockDelta(int index, Map<String, Object> delta) {
        return new StreamEvent(Type.CONTENT_BLOCK_DELTA,
                Map.of("contentBlockIndex", index, "delta", delta));
    }

    public static StreamEvent contentBlockStop(int index) {
        return new StreamEvent(Type.CONTENT_BLOCK_STOP, Map.of("contentBlockIndex", index));
    }

    public static StreamEvent messageStop(String stopReason) {
        return new StreamEvent(Type.MESSAGE_STOP, Map.of("stopReason", stopReason));
    }

    public static StreamEvent metadata(long inputTokens, long outputTokens, long latencyMs) {
        return new StreamEvent(Type.METADATA, Map.of(
                "usage", Map.of("inputTokens", inputTokens, "outputTokens", outputTokens),
                "metrics", Map.of("latencyMs", latencyMs)));
    }

    public Type getType() {
        return type;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
