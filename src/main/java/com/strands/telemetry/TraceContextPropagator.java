package com.strands.telemetry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * W3C Trace Context propagation. Injects/extracts traceparent and tracestate
 * headers for distributed tracing across agent boundaries and MCP calls.
 */
public class TraceContextPropagator {

    private static final String TRACEPARENT = "traceparent";
    private static final String TRACESTATE = "tracestate";
    private static final String VERSION = "00";

    public static Map<String, String> inject(String traceId, String spanId, boolean sampled) {
        Map<String, String> headers = new HashMap<>();
        String flags = sampled ? "01" : "00";
        headers.put(TRACEPARENT, VERSION + "-" + traceId + "-" + spanId + "-" + flags);
        return headers;
    }

    public static Map<String, String> inject(Tracer.Span span) {
        String traceId = generateTraceId();
        String spanId = span.getSpanId().replace("-", "").substring(0, 16);
        return inject(traceId, spanId, true);
    }

    public static TraceContext extract(Map<String, String> headers) {
        String traceparent = headers.get(TRACEPARENT);
        if (traceparent == null) {
            return TraceContext.empty();
        }

        String[] parts = traceparent.split("-");
        if (parts.length < 4) {
            return TraceContext.empty();
        }

        return new TraceContext(
                parts[1],
                parts[2],
                "01".equals(parts[3]),
                headers.getOrDefault(TRACESTATE, "")
        );
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public record TraceContext(String traceId, String parentSpanId, boolean sampled, String traceState) {
        public static TraceContext empty() {
            return new TraceContext(null, null, false, "");
        }

        public boolean isValid() {
            return traceId != null && parentSpanId != null;
        }
    }
}
