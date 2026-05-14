package com.strands.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Real OpenTelemetry integration for Strands agents. Creates proper OTel spans
 * with standard gen_ai semantic conventions.
 */
public class OTelTracer {

    private static final Logger log = LoggerFactory.getLogger(OTelTracer.class);
    private static final String INSTRUMENTATION_NAME = "strands-agents-java";

    private final io.opentelemetry.api.trace.Tracer tracer;
    private final OpenTelemetrySdk sdk;

    private OTelTracer(OpenTelemetrySdk sdk) {
        this.sdk = sdk;
        this.tracer = sdk.getTracer(INSTRUMENTATION_NAME);
    }

    public static OTelTracer create(StrandsTelemetry config) {
        var exporterBuilder = OtlpGrpcSpanExporter.builder();

        if (config.getOtlpEndpoint() != null) {
            exporterBuilder.setEndpoint(config.getOtlpEndpoint());
        }
        for (Map.Entry<String, String> header : config.getOtlpHeaders().entrySet()) {
            exporterBuilder.addHeader(header.getKey(), header.getValue());
        }

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(exporterBuilder.build()).build())
                .setResource(Resource.builder()
                        .put("service.name", config.getServiceName())
                        .build())
                .build();

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();

        return new OTelTracer(sdk);
    }

    public OTelSpan startModelSpan(String modelId, Map<String, String> attributes) {
        Span span = tracer.spanBuilder("chat " + modelId)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("gen_ai.system", "strands-agents")
                .setAttribute("gen_ai.request.model", modelId)
                .startSpan();
        attributes.forEach(span::setAttribute);
        return new OTelSpan(span);
    }

    public OTelSpan startToolSpan(String toolName) {
        Span span = tracer.spanBuilder("execute_tool " + toolName)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("tool.name", toolName)
                .startSpan();
        return new OTelSpan(span);
    }

    public OTelSpan startAgentSpan(String agentName) {
        Span span = tracer.spanBuilder("invoke_agent " + agentName)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("agent.name", agentName)
                .startSpan();
        return new OTelSpan(span);
    }

    public OTelSpan startEventLoopSpan(int cycle) {
        Span span = tracer.spanBuilder("event_loop_cycle")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("event_loop.cycle", cycle)
                .startSpan();
        return new OTelSpan(span);
    }

    public void shutdown() {
        if (sdk != null) {
            sdk.close();
        }
    }

    public static class OTelSpan implements AutoCloseable {
        private final Span span;
        private final Scope scope;

        OTelSpan(Span span) {
            this.span = span;
            this.scope = span.makeCurrent();
        }

        public void setAttribute(String key, String value) {
            span.setAttribute(key, value);
        }

        public void setAttribute(String key, long value) {
            span.setAttribute(key, value);
        }

        public void setError(Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
        }

        public void end() {
            scope.close();
            span.end();
        }

        @Override
        public void close() {
            end();
        }
    }
}
