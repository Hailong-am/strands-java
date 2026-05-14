package com.strands.types;

public enum StopReason {
    END_TURN("end_turn"),
    TOOL_USE("tool_use"),
    MAX_TOKENS("max_tokens"),
    STOP_SEQUENCE("stop_sequence"),
    CANCELLED("cancelled"),
    INTERRUPT("interrupt"),
    GUARDRAIL_INTERVENED("guardrail_intervened"),
    CONTENT_FILTERED("content_filtered"),
    CHECKPOINT("checkpoint");

    private final String value;

    StopReason(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StopReason fromValue(String value) {
        for (StopReason reason : values()) {
            if (reason.value.equals(value)) {
                return reason;
            }
        }
        throw new IllegalArgumentException("Unknown stop reason: " + value);
    }
}
