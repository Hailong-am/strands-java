package com.strands.experimental.bidi;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bidirectional streaming session. Manages the full-duplex communication channel
 * between the client and a real-time model (Nova Sonic, Gemini Live, OpenAI Realtime).
 *
 * Usage:
 * <pre>
 * BidiStreamSession session = new BidiStreamSession(config, handler);
 * session.start();
 * session.sendText("Hello");
 * session.sendAudio(audioBytes, "pcm16");
 * session.close();
 * </pre>
 */
public class BidiStreamSession implements AutoCloseable {

    private final Map<String, Object> config;
    private final BidiStreamHandler handler;
    private final BlockingQueue<BidiStreamEvent> outbound = new LinkedBlockingQueue<>();
    private final AtomicBoolean active = new AtomicBoolean(false);

    public BidiStreamSession(Map<String, Object> config, BidiStreamHandler handler) {
        this.config = config;
        this.handler = handler;
    }

    public void start() {
        active.set(true);
        handler.onEvent(BidiStreamEvent.sessionStart(config));
    }

    public void sendText(String text) {
        if (!active.get()) throw new IllegalStateException("Session not active");
        outbound.offer(BidiStreamEvent.textInput(text));
    }

    public void sendAudio(byte[] data, String format) {
        if (!active.get()) throw new IllegalStateException("Session not active");
        outbound.offer(BidiStreamEvent.audioInput(data, format));
    }

    public void sendToolResult(String result) {
        if (!active.get()) throw new IllegalStateException("Session not active");
        outbound.offer(BidiStreamEvent.toolResult(result));
    }

    public BidiStreamEvent pollOutbound() {
        return outbound.poll();
    }

    public BidiStreamEvent takeOutbound() throws InterruptedException {
        return outbound.take();
    }

    public void receiveEvent(BidiStreamEvent event) {
        handler.onEvent(event);
        switch (event.getType()) {
            case TEXT_OUTPUT -> handler.onTextOutput(event.getText());
            case AUDIO_OUTPUT -> handler.onAudioOutput(event.getAudioData(), event.getAudioFormat());
            case TOOL_CALL -> {
                if (event.getMetadata() != null) {
                    handler.onToolCall(
                            (String) event.getMetadata().get("name"),
                            (String) event.getMetadata().get("toolUseId"),
                            event.getMetadata());
                }
            }
            case ERROR -> handler.onError(event.getText());
            case SESSION_END -> {
                active.set(false);
                handler.onSessionEnd();
            }
            default -> {}
        }
    }

    public boolean isActive() {
        return active.get();
    }

    @Override
    public void close() {
        if (active.compareAndSet(true, false)) {
            handler.onEvent(BidiStreamEvent.sessionEnd());
            handler.onSessionEnd();
        }
    }
}
