package com.strands.telemetry;

import com.strands.types.Message;
import com.strands.types.StopReason;
import com.strands.types.Usage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

public class Tracer {

    private static final Logger log = LoggerFactory.getLogger(Tracer.class);
    private static final Tracer INSTANCE = new Tracer();

    private Tracer() {
    }

    public static Tracer getInstance() {
        return INSTANCE;
    }

    public Span startModelInvokeSpan(String modelId, Map<String, Object> attributes) {
        return new Span("chat", modelId, attributes);
    }

    public void endModelInvokeSpan(Span span, Message message, Usage usage, StopReason stopReason) {
        span.setAttribute("gen_ai.usage.input_tokens", usage.getInputTokens());
        span.setAttribute("gen_ai.usage.output_tokens", usage.getOutputTokens());
        span.setAttribute("gen_ai.response.finish_reason", stopReason.getValue());
        span.end();
    }

    public Span startToolCallSpan(String toolName, Map<String, Object> attributes) {
        return new Span("execute_tool " + toolName, null, attributes);
    }

    public void endToolCallSpan(Span span, boolean success, Exception error) {
        if (error != null) {
            span.setAttribute("error", true);
            span.setAttribute("error.message", error.getMessage());
        }
        span.setAttribute("tool.success", success);
        span.end();
    }

    public Span startEventLoopCycleSpan(Map<String, Object> attributes) {
        return new Span("execute_event_loop_cycle", null, attributes);
    }

    public void endEventLoopCycleSpan(Span span) {
        span.end();
    }

    public Span startAgentSpan(String agentName, String modelId, Map<String, Object> attributes) {
        return new Span("invoke_agent " + agentName, modelId, attributes);
    }

    public void endAgentSpan(Span span, String response, Exception error) {
        if (error != null) {
            span.setAttribute("error", true);
            span.setAttribute("error.message", error.getMessage());
        }
        span.end();
    }

    public Span startSpan(String name) {
        return new Span(name, null, null);
    }

    public static class Span {
        public static final Span NOOP = new Span("noop", null, null) {
            @Override public void setAttribute(String key, Object value) {}
            @Override public void end() {}
        };

        private final String name;
        private final String spanId;
        private final long startTime;
        private final Map<String, Object> attributes;
        private long endTime;

        public Span(String name, String modelId, Map<String, Object> attributes) {
            this.name = name;
            this.spanId = UUID.randomUUID().toString();
            this.startTime = System.currentTimeMillis();
            this.attributes = new java.util.concurrent.ConcurrentHashMap<>();
            if (modelId != null) {
                this.attributes.put("gen_ai.request.model", modelId);
            }
            this.attributes.put("gen_ai.system", "strands-agents");
            if (attributes != null) {
                this.attributes.putAll(attributes);
            }
        }

        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }

        public void end() {
            this.endTime = System.currentTimeMillis();
            log.debug("Span [{}] {} completed in {}ms", spanId, name, endTime - startTime);
        }

        public String getName() {
            return name;
        }

        public String getSpanId() {
            return spanId;
        }

        public long getDurationMs() {
            return (endTime > 0 ? endTime : System.currentTimeMillis()) - startTime;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }
}
