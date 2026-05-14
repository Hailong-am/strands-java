package com.strands.plugin.steering;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Isolated, versioned key-value context store for steering decisions.
 * Supports deep copy and version tracking for audit purposes.
 */
public class SteeringContext {

    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private final AtomicLong version = new AtomicLong(0);

    public void set(String key, Object value) {
        data.put(key, value);
        version.incrementAndGet();
    }

    public Object get(String key) {
        return data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) data.get(key);
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public void remove(String key) {
        data.remove(key);
        version.incrementAndGet();
    }

    public long getVersion() {
        return version.get();
    }

    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }

    public SteeringContext copy() {
        SteeringContext copy = new SteeringContext();
        copy.data.putAll(this.data);
        return copy;
    }

    public void clear() {
        data.clear();
        version.incrementAndGet();
    }
}
