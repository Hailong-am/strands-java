package com.strands.model.llamaapi;

import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.model.openai.OpenAIModel;
import com.strands.types.streaming.StreamEvent;

import java.util.Iterator;
import java.util.Map;

/**
 * Llama API model. Uses the OpenAI-compatible API endpoint at api.llama.com.
 */
public class LlamaAPIModel implements Model {

    private static final String DEFAULT_BASE_URL = "https://api.llama.com/v1";
    private static final String DEFAULT_MODEL_ID = "llama-3.3-70b";

    private final OpenAIModel delegate;

    public LlamaAPIModel(String apiKey) {
        this(apiKey, DEFAULT_MODEL_ID);
    }

    public LlamaAPIModel(String apiKey, String modelId) {
        this(apiKey, DEFAULT_BASE_URL, modelId);
    }

    public LlamaAPIModel(String apiKey, String baseUrl, String modelId) {
        this.delegate = new OpenAIModel(apiKey, baseUrl, modelId);
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
