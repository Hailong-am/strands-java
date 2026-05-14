package com.strands.telemetry;

import com.strands.types.Message;
import com.strands.types.StopReason;
import com.strands.types.Usage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TracerTest {

    @Test
    void testSingletonInstance() {
        Tracer t1 = Tracer.getInstance();
        Tracer t2 = Tracer.getInstance();
        assertSame(t1, t2);
    }

    @Test
    void testStartModelInvokeSpan() {
        Tracer.Span span = Tracer.getInstance().startModelInvokeSpan(
                "claude-sonnet", Map.of("temperature", "0.7"));

        assertNotNull(span.getSpanId());
        assertEquals("chat", span.getName());
        assertEquals("claude-sonnet", span.getAttributes().get("gen_ai.request.model"));
        assertEquals("strands-agents", span.getAttributes().get("gen_ai.system"));
    }

    @Test
    void testEndModelInvokeSpan() {
        Tracer tracer = Tracer.getInstance();
        Tracer.Span span = tracer.startModelInvokeSpan("claude", Map.of());
        Usage usage = new Usage(100, 50);

        tracer.endModelInvokeSpan(span, Message.assistant("hi"), usage, StopReason.END_TURN);

        assertEquals(100L, span.getAttributes().get("gen_ai.usage.input_tokens"));
        assertEquals(50L, span.getAttributes().get("gen_ai.usage.output_tokens"));
        assertEquals("end_turn", span.getAttributes().get("gen_ai.response.finish_reason"));
        assertTrue(span.getDurationMs() >= 0);
    }

    @Test
    void testStartToolCallSpan() {
        Tracer.Span span = Tracer.getInstance().startToolCallSpan(
                "calculator", Map.of("a", "1"));

        assertEquals("execute_tool calculator", span.getName());
    }

    @Test
    void testEndToolCallSpanSuccess() {
        Tracer tracer = Tracer.getInstance();
        Tracer.Span span = tracer.startToolCallSpan("search", Map.of());
        tracer.endToolCallSpan(span, true, null);

        assertEquals(true, span.getAttributes().get("tool.success"));
        assertNull(span.getAttributes().get("error"));
    }

    @Test
    void testEndToolCallSpanFailure() {
        Tracer tracer = Tracer.getInstance();
        Tracer.Span span = tracer.startToolCallSpan("search", Map.of());
        tracer.endToolCallSpan(span, false, new RuntimeException("network error"));

        assertEquals(false, span.getAttributes().get("tool.success"));
        assertEquals(true, span.getAttributes().get("error"));
        assertEquals("network error", span.getAttributes().get("error.message"));
    }

    @Test
    void testStartSpan() {
        Tracer.Span span = Tracer.getInstance().startSpan("custom-op");
        assertNotNull(span);
        assertEquals("custom-op", span.getName());
    }

    @Test
    void testNoopSpanDoesNothing() {
        Tracer.Span noop = Tracer.Span.NOOP;
        assertDoesNotThrow(() -> noop.setAttribute("key", "value"));
        assertDoesNotThrow(noop::end);
    }

    @Test
    void testSpanDuration() throws InterruptedException {
        Tracer.Span span = Tracer.getInstance().startSpan("timed");
        Thread.sleep(10);
        span.end();
        assertTrue(span.getDurationMs() >= 10);
    }

    @Test
    void testEventLoopCycleSpan() {
        Tracer tracer = Tracer.getInstance();
        Tracer.Span span = tracer.startEventLoopCycleSpan(Map.of("cycle", "1"));
        assertNotNull(span);
        assertEquals("execute_event_loop_cycle", span.getName());
        tracer.endEventLoopCycleSpan(span);
    }

    @Test
    void testAgentSpan() {
        Tracer tracer = Tracer.getInstance();
        Tracer.Span span = tracer.startAgentSpan("my-agent", "claude-3", Map.of());
        assertEquals("invoke_agent my-agent", span.getName());
        assertEquals("claude-3", span.getAttributes().get("gen_ai.request.model"));

        tracer.endAgentSpan(span, "result", null);
    }

    @Test
    void testAgentSpanWithError() {
        Tracer tracer = Tracer.getInstance();
        Tracer.Span span = tracer.startAgentSpan("my-agent", "claude-3", Map.of());

        tracer.endAgentSpan(span, null, new RuntimeException("timeout"));
        assertEquals(true, span.getAttributes().get("error"));
        assertEquals("timeout", span.getAttributes().get("error.message"));
    }
}
