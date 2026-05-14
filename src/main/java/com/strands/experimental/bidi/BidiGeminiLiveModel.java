package com.strands.experimental.bidi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Bidirectional model implementation for Google Gemini Live.
 * Connects via WebSocket for real-time multimodal streaming.
 */
public class BidiGeminiLiveModel implements BidiModel {

    private static final Logger log = LoggerFactory.getLogger(BidiGeminiLiveModel.class);

    private final String modelId;
    private final String apiKey;

    public BidiGeminiLiveModel(String apiKey) {
        this("gemini-2.0-flash-live", apiKey);
    }

    public BidiGeminiLiveModel(String modelId, String apiKey) {
        this.modelId = modelId;
        this.apiKey = apiKey;
    }

    @Override
    public BidiStreamSession openSession(Map<String, Object> config, BidiStreamHandler handler) {
        Map<String, Object> sessionConfig = new HashMap<>(config);
        sessionConfig.put("modelId", modelId);
        sessionConfig.put("provider", "gemini-live");
        sessionConfig.put("audioFormat", config.getOrDefault("audioFormat", "pcm16"));

        log.debug("Opening Gemini Live bidi session: model={}", modelId);
        return new BidiStreamSession(sessionConfig, handler);
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    public boolean supportsAudio() {
        return true;
    }
}
