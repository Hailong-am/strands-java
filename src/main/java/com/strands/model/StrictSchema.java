package com.strands.model;

import java.util.*;

/**
 * Utilities for making JSON schemas strict (required for some model providers
 * like OpenAI's structured outputs). Adds "additionalProperties": false
 * recursively and ensures all properties are in "required".
 */
public class StrictSchema {

    @SuppressWarnings("unchecked")
    public static Map<String, Object> makeStrict(Map<String, Object> schema) {
        if (schema == null) return null;

        Map<String, Object> result = new LinkedHashMap<>(schema);

        String type = (String) result.get("type");
        if ("object".equals(type)) {
            result.put("additionalProperties", false);

            Map<String, Object> properties = (Map<String, Object>) result.get("properties");
            if (properties != null) {
                List<String> allKeys = new ArrayList<>(properties.keySet());
                result.put("required", allKeys);

                Map<String, Object> strictProps = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    if (entry.getValue() instanceof Map) {
                        strictProps.put(entry.getKey(), makeStrict((Map<String, Object>) entry.getValue()));
                    } else {
                        strictProps.put(entry.getKey(), entry.getValue());
                    }
                }
                result.put("properties", strictProps);
            }
        } else if ("array".equals(type)) {
            Map<String, Object> items = (Map<String, Object>) result.get("items");
            if (items != null) {
                result.put("items", makeStrict(items));
            }
        }

        return result;
    }

    public static boolean isStrict(Map<String, Object> schema) {
        if (schema == null) return false;
        if ("object".equals(schema.get("type"))) {
            return Boolean.FALSE.equals(schema.get("additionalProperties"));
        }
        return true;
    }
}
