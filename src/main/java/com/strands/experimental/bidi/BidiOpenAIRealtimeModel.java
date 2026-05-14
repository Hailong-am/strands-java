package com.strands.experimental.bidi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Bidirectional model implementation for OpenAI Realtime API.
 * Connects via WebSocket for real-time voice conversation.
 */
public class BidiOpenAIRealtimeModel implements BidiModel {

    private static final Logger log = LoggerFactory.getLogger(BidiOpenAIRealtimeModel.class);

    private final String modelId;
    private final String apiKey;
    private final String baseUrl;

    public BidiOpenAIRealtimeModel(String apiKey) {
        this("gpt-4o-realtime", apiKey, "wss://api.openai.com/v1/realtime");
    }

    public BidiOpenAIRealtimeModel(String modelId, String apiKey, String baseUrl) {
        this.modelId = modelId;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public BidiStreamSession openSession(Map<String, Object> config, BidiStreamHandler handler) {
        Map<String, Object> sessionConfig = new HashMap<>(config);
        sessionConfig.put("modelId", modelId);
        sessionConfig.put("provider", "openai-realtime");
        sessionConfig.put("baseUrl", baseUrl);
        sessionConfig.put("audioFormat", config.getOrDefault("audioFormat", "pcm16"));
        sessionConfig.put("voice", config.getOrDefault("voice", "alloy"));

        log.debug("Opening OpenAI Realtime bidi session: model={}", modelId);
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
