package com.strands.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates model configuration parameters before sending requests.
 */
public class ModelValidation {

    public static List<String> validate(ModelConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getModelId() == null || config.getModelId().isBlank()) {
            errors.add("modelId is required");
        }

        Map<String, Object> params = config.getParameters();
        if (params.containsKey("temperature")) {
            Object temp = params.get("temperature");
            if (temp instanceof Number n) {
                if (n.doubleValue() < 0 || n.doubleValue() > 2.0) {
                    errors.add("temperature must be between 0 and 2.0");
                }
            }
        }
        if (params.containsKey("topP")) {
            Object topP = params.get("topP");
            if (topP instanceof Number n) {
                if (n.doubleValue() < 0 || n.doubleValue() > 1.0) {
                    errors.add("topP must be between 0 and 1.0");
                }
            }
        }
        if (params.containsKey("maxTokens")) {
            Object maxTokens = params.get("maxTokens");
            if (maxTokens instanceof Number n) {
                if (n.intValue() <= 0) {
                    errors.add("maxTokens must be positive");
                }
            }
        }

        return errors;
    }

    public static void validateOrThrow(ModelConfig config) {
        List<String> errors = validate(config);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid model config: " + String.join("; ", errors));
        }
    }
}
