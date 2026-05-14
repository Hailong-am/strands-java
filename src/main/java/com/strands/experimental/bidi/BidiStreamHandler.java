package com.strands.experimental.bidi;

/**
 * Handler for bidirectional streaming events. Implement this to process
 * real-time text and audio from a bidirectional model session.
 */
public interface BidiStreamHandler {

    void onEvent(BidiStreamEvent event);

    default void onTextOutput(String text) {}

    default void onAudioOutput(byte[] data, String format) {}

    default void onToolCall(String toolName, String toolUseId, java.util.Map<String, Object> input) {}

    default void onError(String message) {}

    default void onSessionEnd() {}
}
