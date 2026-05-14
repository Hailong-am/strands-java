package com.strands.event;

import com.strands.types.Metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InvocationState {

    private final Map<String, Object> properties;
    private final Metrics metrics;

    public InvocationState() {
        this.properties = new ConcurrentHashMap<>();
        this.metrics = new Metrics();
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void set(String key, Object value) {
        properties.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) properties.get(key);
    }

    public Metrics getMetrics() {
        return metrics;
    }
}
