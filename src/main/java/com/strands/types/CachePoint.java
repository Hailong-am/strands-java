package com.strands.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a cache breakpoint in the prompt. Used to tell the model where
 * caching boundaries should be for prompt prefix caching optimization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CachePoint {

    private String type = "default";
    private Integer ttlSeconds;

    public static CachePoint defaultPoint() {
        return new CachePoint("default", null);
    }

    public static CachePoint withTtl(int ttlSeconds) {
        return new CachePoint("default", ttlSeconds);
    }
}
