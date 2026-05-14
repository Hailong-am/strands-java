package com.strands.experimental.bidi;

import java.util.Map;

/**
 * Interface for bidirectional streaming models that support real-time
 * full-duplex communication (text + audio). Implementations connect to
 * Nova Sonic, Gemini Live, or OpenAI Realtime APIs.
 */
public interface BidiModel {

    BidiStreamSession openSession(Map<String, Object> config, BidiStreamHandler handler);

    default String getModelId() {
        return "unknown";
    }

    default boolean supportsAudio() {
        return true;
    }
}
