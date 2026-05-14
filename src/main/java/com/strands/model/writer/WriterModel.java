package com.strands.model.writer;

import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.model.openai.OpenAIModel;
import com.strands.types.streaming.StreamEvent;

import java.util.Iterator;
import java.util.Map;

/**
 * Writer AI model. Writer exposes an OpenAI-compatible API for their Palmyra models.
 */
public class WriterModel implements Model {

    private static final String DEFAULT_BASE_URL = "https://api.writer.com/v1";
    private static final String DEFAULT_MODEL_ID = "palmyra-x-004";

    private final OpenAIModel delegate;

    public WriterModel(String apiKey) {
        this(apiKey, DEFAULT_MODEL_ID);
    }

    public WriterModel(String apiKey, String modelId) {
        this(apiKey, DEFAULT_BASE_URL, modelId);
    }

    public WriterModel(String apiKey, String baseUrl, String modelId) {
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
