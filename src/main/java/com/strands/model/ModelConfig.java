package com.strands.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ModelConfig {

    @Setter
    private String modelId;
    private final Map<String, Object> parameters;

    public ModelConfig() {
        this.parameters = new HashMap<>();
    }

    public ModelConfig(String modelId) {
        this.modelId = modelId;
        this.parameters = new HashMap<>();
    }

    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        return (T) parameters.get(key);
    }
}
