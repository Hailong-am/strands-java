package com.strands.experimental.bidi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Bidirectional model implementation for Amazon Nova Sonic.
 * Connects via Bedrock's bidirectional streaming API for real-time
 * voice and text interactions.
 */
public class BidiNovaSonicModel implements BidiModel {

    private static final Logger log = LoggerFactory.getLogger(BidiNovaSonicModel.class);

    private final String modelId;
    private final String region;

    public BidiNovaSonicModel() {
        this("amazon.nova-sonic-v1:0", "us-east-1");
    }

    public BidiNovaSonicModel(String modelId, String region) {
        this.modelId = modelId;
        this.region = region;
    }

    @Override
    public BidiStreamSession openSession(Map<String, Object> config, BidiStreamHandler handler) {
        Map<String, Object> sessionConfig = new HashMap<>(config);
        sessionConfig.put("modelId", modelId);
        sessionConfig.put("region", region);
        sessionConfig.put("provider", "nova-sonic");
        sessionConfig.put("audioFormat", config.getOrDefault("audioFormat", "pcm16"));
        sessionConfig.put("sampleRate", config.getOrDefault("sampleRate", 16000));

        log.debug("Opening Nova Sonic bidi session: model={}, region={}", modelId, region);
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
