package com.strands.plugin.steering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Audit trail for steering decisions. Records every handler evaluation
 * with its phase, action, and context version for debugging and compliance.
 */
public class LedgerProvider {

    private final List<LedgerEntry> entries = new CopyOnWriteArrayList<>();

    public void record(String phase, String handlerName, SteeringAction action, long contextVersion) {
        entries.add(LedgerEntry.of(phase, handlerName, action, contextVersion));
    }

    public List<LedgerEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public List<LedgerEntry> getEntriesByPhase(String phase) {
        return entries.stream()
                .filter(e -> e.phase().equals(phase))
                .toList();
    }

    public List<LedgerEntry> getEntriesByHandler(String handlerName) {
        return entries.stream()
                .filter(e -> e.handlerName().equals(handlerName))
                .toList();
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }
}
