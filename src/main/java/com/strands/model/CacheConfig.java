package com.strands.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CacheConfig {

    @Builder.Default
    private final boolean enabled = true;

    @Builder.Default
    private final int ttlSeconds = 300;

    @Builder.Default
    private final CacheScope scope = CacheScope.SYSTEM_PROMPT;

    public enum CacheScope {
        SYSTEM_PROMPT,
        TOOLS,
        ALL
    }
}
