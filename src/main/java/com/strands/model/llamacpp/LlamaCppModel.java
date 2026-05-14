package com.strands.model.llamacpp;

import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.model.openai.OpenAIModel;
import com.strands.types.streaming.StreamEvent;

import java.util.Iterator;
import java.util.Map;

/**
 * llama.cpp model. Connects to a local llama.cpp server that exposes an
 * OpenAI-compatible chat completions endpoint.
 */
public class LlamaCppModel implements Model {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080/v1";
    private static final String DEFAULT_MODEL_ID = "local";

    private final OpenAIModel delegate;

    public LlamaCppModel() {
        this(DEFAULT_BASE_URL, DEFAULT_MODEL_ID);
    }

    public LlamaCppModel(String baseUrl) {
        this(baseUrl, DEFAULT_MODEL_ID);
    }

    public LlamaCppModel(String baseUrl, String modelId) {
        this.delegate = new OpenAIModel("not-needed", baseUrl, modelId);
    }

    @Override
    public Iterator<StreamEvent> stream(StreamRequest request) {
        return delegate.stream(request);
    }

    @Override
    public ModelConfig getConfig() {
        return delegate.getConfig();
    }

    @Override
    public void updateConfig(Map<String, Object> configUpdates) {
        delegate.updateConfig(configUpdates);
    }
}
