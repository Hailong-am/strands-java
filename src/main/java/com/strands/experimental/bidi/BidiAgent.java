package com.strands.experimental.bidi;

import com.strands.tool.AgentTool;
import com.strands.tool.ToolContext;
import com.strands.tool.ToolRegistry;
import com.strands.types.ToolResult;
import com.strands.types.ToolUse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Agent that operates over a bidirectional streaming session.
 * Handles the event loop for real-time models: receives events, executes
 * tool calls, and sends results back through the session.
 */
public class BidiAgent implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(BidiAgent.class);

    private final BidiModel model;
    private final ToolRegistry toolRegistry;
    private final String systemPrompt;
    private BidiStreamSession session;
    private volatile boolean running;

    private BidiAgent(Builder builder) {
        this.model = builder.model;
        this.toolRegistry = new ToolRegistry();
        this.systemPrompt = builder.systemPrompt;
        if (builder.tools != null) {
            builder.tools.forEach(toolRegistry::register);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public BidiStreamSession start(BidiStreamHandler handler) {
        return start(handler, Map.of());
    }

    public BidiStreamSession start(BidiStreamHandler handler, Map<String, Object> config) {
        Map<String, Object> sessionConfig = new HashMap<>(config);
        if (systemPrompt != null) {
            sessionConfig.put("systemPrompt", systemPrompt);
        }
        sessionConfig.put("tools", toolRegistry.getToolDefinitions());

        session = model.openSession(sessionConfig, new ToolExecutingHandler(handler));
        running = true;
        session.start();
        return session;
    }

    public void sendText(String text) {
        if (session == null || !session.isActive()) {
            throw new IllegalStateException("No active session");
        }
        session.sendText(text);
    }

    public void sendAudio(byte[] data, String format) {
        if (session == null || !session.isActive()) {
            throw new IllegalStateException("No active session");
        }
        session.sendAudio(data, format);
    }

    public boolean isRunning() {
        return running && session != null && session.isActive();
    }

    @Override
    public void close() {
        running = false;
        if (session != null) {
            session.close();
        }
    }

    private class ToolExecutingHandler implements BidiStreamHandler {
        private final BidiStreamHandler delegate;

        ToolExecutingHandler(BidiStreamHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onEvent(BidiStreamEvent event) {
            delegate.onEvent(event);
        }

        @Override
        public void onTextOutput(String text) {
            delegate.onTextOutput(text);
        }

        @Override
        public void onAudioOutput(byte[] data, String format) {
            delegate.onAudioOutput(data, format);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onToolCall(String toolName, String toolUseId, Map<String, Object> input) {
            delegate.onToolCall(toolName, toolUseId, input);

            AgentTool tool = toolRegistry.get(toolName);
            if (tool == null) {
                log.warn("Unknown tool requested: {}", toolName);
                session.sendToolResult("{\"error\": \"Unknown tool: " + toolName + "\"}");
                return;
            }

            CompletableFuture.runAsync(() -> {
                try {
                    ToolUse toolUse = new ToolUse(toolUseId, toolName, input);
                    ToolContext ctx = new ToolContext(toolUse, new HashMap<>());
                    ToolResult result = tool.invoke(toolUse, ctx);
                    String resultText = result.getContent() != null && !result.getContent().isEmpty()
                            ? result.getContent().get(0).getText()
                            : "";
                    session.sendToolResult(resultText);
                } catch (Exception e) {
                    log.error("Tool execution failed: {}", toolName, e);
                    session.sendToolResult("{\"error\": \"" + e.getMessage() + "\"}");
                }
            });
        }

        @Override
        public void onError(String message) {
            delegate.onError(message);
        }

        @Override
        public void onSessionEnd() {
            running = false;
            delegate.onSessionEnd();
        }
    }

    public static class Builder {
        private BidiModel model;
        private List<AgentTool> tools;
        private String systemPrompt;

        public Builder model(BidiModel model) {
            this.model = model;
            return this;
        }

        public Builder tools(AgentTool... tools) {
            this.tools = List.of(tools);
            return this;
        }

        public Builder tools(List<AgentTool> tools) {
            this.tools = tools;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public BidiAgent build() {
            if (model == null) {
                throw new IllegalStateException("BidiModel is required");
            }
            return new BidiAgent(this);
        }
    }
}
