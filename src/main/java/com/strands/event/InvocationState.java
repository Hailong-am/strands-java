package com.strands.event;

import com.strands.types.Metrics;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class InvocationState {

    private final Map<String, Object> properties = new ConcurrentHashMap<>();
    private final Metrics metrics = new Metrics();

    public void set(String key, Object value) {
        properties.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) properties.get(key);
    }
}
