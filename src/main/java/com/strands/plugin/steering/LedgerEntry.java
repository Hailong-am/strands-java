package com.strands.plugin.steering;

import java.time.Instant;

public record LedgerEntry(
        Instant timestamp,
        String phase,
        String handlerName,
        SteeringAction action,
        long contextVersion
) {
    public static LedgerEntry of(String phase, String handlerName, SteeringAction action, long contextVersion) {
        return new LedgerEntry(Instant.now(), phase, handlerName, action, contextVersion);
    }
}
