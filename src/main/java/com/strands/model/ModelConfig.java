package com.strands.model;

import java.util.HashMap;
import java.util.Map;

public class ModelConfig {

    private String modelId;
    private final Map<String, Object> parameters;

    public ModelConfig() {
        this.parameters = new HashMap<>();
    }

    public ModelConfig(String modelId) {
        this.modelId = modelId;
        this.parameters = new HashMap<>();
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        return (T) parameters.get(key);
    }
}
