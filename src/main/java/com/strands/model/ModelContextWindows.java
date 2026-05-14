package com.strands.model;

import java.util.Map;

/**
 * Lookup table for model context window sizes. Used by Model.getContextWindowLimit()
 * to provide token limits without requiring API calls.
 */
public final class ModelContextWindows {

    private static final int DEFAULT_LIMIT = 128_000;

    private static final Map<String, Integer> LIMITS = Map.ofEntries(
            Map.entry("anthropic.claude-3-5-sonnet-20241022-v2:0", 200_000),
            Map.entry("anthropic.claude-3-5-haiku-20241022-v1:0", 200_000),
            Map.entry("anthropic.claude-3-opus-20240229-v1:0", 200_000),
            Map.entry("anthropic.claude-3-sonnet-20240229-v1:0", 200_000),
            Map.entry("anthropic.claude-3-haiku-20240307-v1:0", 200_000),
            Map.entry("anthropic.claude-sonnet-4-20250514-v1:0", 200_000),
            Map.entry("anthropic.claude-opus-4-20250514-v1:0", 200_000),
            Map.entry("claude-sonnet-4-20250514", 200_000),
            Map.entry("claude-opus-4-20250514", 200_000),
            Map.entry("claude-3-5-sonnet-20241022", 200_000),
            Map.entry("claude-3-5-haiku-20241022", 200_000),
            Map.entry("amazon.nova-pro-v1:0", 300_000),
            Map.entry("amazon.nova-lite-v1:0", 300_000),
            Map.entry("amazon.nova-micro-v1:0", 128_000),
            Map.entry("meta.llama3-1-405b-instruct-v1:0", 128_000),
            Map.entry("meta.llama3-1-70b-instruct-v1:0", 128_000),
            Map.entry("meta.llama3-1-8b-instruct-v1:0", 128_000),
            Map.entry("gpt-4o", 128_000),
            Map.entry("gpt-4o-mini", 128_000),
            Map.entry("gpt-4-turbo", 128_000),
            Map.entry("o1", 200_000),
            Map.entry("o1-mini", 128_000)
    );

    private ModelContextWindows() {}

    public static int getLimit(String modelId) {
        if (modelId == null) return DEFAULT_LIMIT;

        Integer limit = LIMITS.get(modelId);
        if (limit != null) return limit;

        for (Map.Entry<String, Integer> entry : LIMITS.entrySet()) {
            if (modelId.contains(entry.getKey()) || entry.getKey().contains(modelId)) {
                return entry.getValue();
            }
        }

        return DEFAULT_LIMIT;
    }
}
