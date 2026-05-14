package com.strands.telemetry;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StrandsTelemetryTest {

    @Test
    void testSingletonInstance() {
        StrandsTelemetry t1 = StrandsTelemetry.instance();
        StrandsTelemetry t2 = StrandsTelemetry.instance();
        assertSame(t1, t2);
    }

    @Test
    void testEnableDisable() {
        StrandsTelemetry telemetry = StrandsTelemetry.instance();
        telemetry.disable();
        assertFalse(telemetry.isEnabled());

        telemetry.enable();
        assertTrue(telemetry.isEnabled());

        telemetry.disable();
    }

    @Test
    void testServiceName() {
        StrandsTelemetry telemetry = StrandsTelemetry.instance();
        telemetry.serviceName("my-service");
        assertEquals("my-service", telemetry.getServiceName());
        telemetry.serviceName("strands-agents");
    }

    @Test
    void testOtlpEndpointEnablesTelemetry() {
        StrandsTelemetry telemetry = StrandsTelemetry.instance();
        telemetry.disable();
        telemetry.otlpEndpoint("http://localhost:4317");
        assertTrue(telemetry.isEnabled());
        assertEquals("http://localhost:4317", telemetry.getOtlpEndpoint());
        telemetry.disable();
    }

    @Test
    void testOtlpHeaders() {
        StrandsTelemetry telemetry = StrandsTelemetry.instance();
        telemetry.otlpHeader("Authorization", "Bearer token");
        assertEquals("Bearer token", telemetry.getOtlpHeaders().get("Authorization"));
        telemetry.getOtlpHeaders().remove("Authorization");
    }

    @Test
    void testConsoleExporter() {
        StrandsTelemetry telemetry = StrandsTelemetry.instance();
        assertFalse(telemetry.isConsoleExporter());
        telemetry.consoleExporter(true);
        assertTrue(telemetry.isConsoleExporter());
        telemetry.consoleExporter(false);
    }

    @Test
    void testStartSpanWhenDisabled() {
        StrandsTelemetry telemetry = StrandsTelemetry.instance();
        telemetry.disable();

        Tracer.Span span = telemetry.startSpan("test");
        assertSame(Tracer.Span.NOOP, span);
    }

    @Test
    void testStartSpanWhenEnabled() {
        StrandsTelemetry telemetry = StrandsTelemetry.instance();
        telemetry.enable();

        Tracer.Span span = telemetry.startSpan("test-op", Map.of("key", "value"));
        assertNotSame(Tracer.Span.NOOP, span);
        assertEquals("test-op", span.getName());

        telemetry.disable();
    }
}
