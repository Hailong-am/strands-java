package com.strands.experimental.bidi;

import java.util.Map;

/**
 * Event for bidirectional streaming communication. Supports both text and audio
 * modalities for real-time interaction with models like Nova Sonic, Gemini Live,
 * and OpenAI Realtime.
 */
public class BidiStreamEvent {

    public enum Type {
        TEXT_INPUT, TEXT_OUTPUT,
        AUDIO_INPUT, AUDIO_OUTPUT,
        TOOL_CALL, TOOL_RESULT,
        SESSION_START, SESSION_END,
        ERROR
    }

    public enum Modality {
        TEXT, AUDIO
    }

    private final Type type;
    private final Modality modality;
    private final String text;
    private final byte[] audioData;
    private final String audioFormat;
    private final Map<String, Object> metadata;

    private BidiStreamEvent(Type type, Modality modality, String text,
                            byte[] audioData, String audioFormat, Map<String, Object> metadata) {
        this.type = type;
        this.modality = modality;
        this.text = text;
        this.audioData = audioData;
        this.audioFormat = audioFormat;
        this.metadata = metadata;
    }

    public static BidiStreamEvent textInput(String text) {
        return new BidiStreamEvent(Type.TEXT_INPUT, Modality.TEXT, text, null, null, null);
    }

    public static BidiStreamEvent textOutput(String text) {
        return new BidiStreamEvent(Type.TEXT_OUTPUT, Modality.TEXT, text, null, null, null);
    }

    public static BidiStreamEvent audioInput(byte[] data, String format) {
        return new BidiStreamEvent(Type.AUDIO_INPUT, Modality.AUDIO, null, data, format, null);
    }

    public static BidiStreamEvent audioOutput(byte[] data, String format) {
        return new BidiStreamEvent(Type.AUDIO_OUTPUT, Modality.AUDIO, null, data, format, null);
    }

    public static BidiStreamEvent toolCall(Map<String, Object> metadata) {
        return new BidiStreamEvent(Type.TOOL_CALL, Modality.TEXT, null, null, null, metadata);
    }

    public static BidiStreamEvent toolResult(String result) {
        return new BidiStreamEvent(Type.TOOL_RESULT, Modality.TEXT, result, null, null, null);
    }

    public static BidiStreamEvent sessionStart(Map<String, Object> config) {
        return new BidiStreamEvent(Type.SESSION_START, null, null, null, null, config);
    }

    public static BidiStreamEvent sessionEnd() {
        return new BidiStreamEvent(Type.SESSION_END, null, null, null, null, null);
    }

    public static BidiStreamEvent error(String message) {
        return new BidiStreamEvent(Type.ERROR, null, message, null, null, null);
    }

    public Type getType() { return type; }
    public Modality getModality() { return modality; }
    public String getText() { return text; }
    public byte[] getAudioData() { return audioData; }
    public String getAudioFormat() { return audioFormat; }
    public Map<String, Object> getMetadata() { return metadata; }
}
