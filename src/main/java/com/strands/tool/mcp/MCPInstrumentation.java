package com.strands.tool.mcp;

import com.strands.telemetry.OTelTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Instruments MCP client-server communication with OpenTelemetry tracing.
 * Wraps tool calls with spans that propagate context across process boundaries.
 */
public class MCPInstrumentation {

    private static final Logger log = LoggerFactory.getLogger(MCPInstrumentation.class);

    private final OTelTracer tracer;

    public MCPInstrumentation(OTelTracer tracer) {
        this.tracer = tracer;
    }

    public Map<String, Object> instrumentToolCall(String toolName, Map<String, Object> arguments,
                                                   MCPClient client) {
        OTelTracer.OTelSpan span = tracer.startToolSpan("mcp." + toolName);
        span.setAttribute("mcp.tool.name", toolName);
        span.setAttribute("mcp.transport", client.getClass().getSimpleName());

        try {
            Map<String, Object> result = client.callTool(toolName, arguments);
            span.setAttribute("mcp.tool.success", "true");
            return result;
        } catch (Exception e) {
            span.setError(e);
            span.setAttribute("mcp.tool.success", "false");
            throw e;
        } finally {
            span.end();
        }
    }
}
