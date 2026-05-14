package com.strands.model.litellm;

import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.model.openai.OpenAIModel;
import com.strands.types.streaming.StreamEvent;

import java.util.Iterator;
import java.util.Map;

/**
 * LiteLLM proxy model. LiteLLM exposes an OpenAI-compatible API that routes
 * to 100+ model providers. This delegates to OpenAIModel with a configurable base URL.
 */
public class LiteLLMModel implements Model {

    private final OpenAIModel delegate;

    public LiteLLMModel(String baseUrl, String apiKey, String modelId) {
        this.delegate = new OpenAIModel(apiKey, baseUrl, modelId);
    }

    public LiteLLMModel(String baseUrl, String modelId) {
        this(baseUrl, "not-needed", modelId);
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
