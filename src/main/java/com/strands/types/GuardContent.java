package com.strands.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents guardrail assessment content returned by the model when a
 * guardrail has been triggered.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GuardContent {

    private String guardId;
    private String type;
    private String action;
    private double confidence;
    private String message;
    private Map<String, Object> details;

    public boolean isBlocked() {
        return "BLOCKED".equalsIgnoreCase(action);
    }

    public boolean isWarning() {
        return "WARNING".equalsIgnoreCase(action) || "INTERVENED".equalsIgnoreCase(action);
    }
}
