package com.strands.agent;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentState {

    private final Map<String, Object> data = new ConcurrentHashMap<>();

    public void set(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) data.get(key);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public void remove(String key) {
        data.remove(key);
    }

    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(data);
    }

    public void loadFrom(Map<String, Object> map) {
        data.clear();
        if (map != null) {
            data.putAll(map);
        }
    }
}
