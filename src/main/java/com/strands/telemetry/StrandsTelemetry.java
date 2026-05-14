package com.strands.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StrandsTelemetry {

    private static final Logger log = LoggerFactory.getLogger(StrandsTelemetry.class);
    private static final StrandsTelemetry INSTANCE = new StrandsTelemetry();

    private boolean enabled;
    private String serviceName = "strands-agents";
    private String otlpEndpoint;
    private Map<String, String> otlpHeaders = new ConcurrentHashMap<>();
    private boolean consoleExporter;

    private StrandsTelemetry() {
        String envEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (envEndpoint != null) {
            this.otlpEndpoint = envEndpoint;
            this.enabled = true;
        }
        String envService = System.getenv("OTEL_SERVICE_NAME");
        if (envService != null) {
            this.serviceName = envService;
        }
        String envHeaders = System.getenv("OTEL_EXPORTER_OTLP_HEADERS");
        if (envHeaders != null) {
            for (String pair : envHeaders.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    otlpHeaders.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
    }

    public static StrandsTelemetry instance() {
        return INSTANCE;
    }

    public StrandsTelemetry enable() {
        this.enabled = true;
        return this;
    }

    public StrandsTelemetry disable() {
        this.enabled = false;
        return this;
    }

    public StrandsTelemetry serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public StrandsTelemetry otlpEndpoint(String endpoint) {
        this.otlpEndpoint = endpoint;
        this.enabled = true;
        return this;
    }

    public StrandsTelemetry otlpHeader(String key, String value) {
        this.otlpHeaders.put(key, value);
        return this;
    }

    public StrandsTelemetry consoleExporter(boolean enabled) {
        this.consoleExporter = enabled;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getOtlpEndpoint() {
        return otlpEndpoint;
    }

    public Map<String, String> getOtlpHeaders() {
        return otlpHeaders;
    }

    public boolean isConsoleExporter() {
        return consoleExporter;
    }

    public Tracer.Span startSpan(String name) {
        return startSpan(name, Map.of());
    }

    public Tracer.Span startSpan(String name, Map<String, String> attributes) {
        if (!enabled) return Tracer.Span.NOOP;
        Tracer.Span span = Tracer.getInstance().startSpan(name);
        attributes.forEach(span::setAttribute);
        if (consoleExporter) {
            log.info("[OTEL] span.start: {} attrs={}", name, attributes);
        }
        return span;
    }
}
