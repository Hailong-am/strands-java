package com.strands.telemetry;

/**
 * Standard OpenTelemetry semantic convention attribute names for gen_ai operations.
 */
public final class MetricsConstants {

    private MetricsConstants() {}

    // GenAI request attributes
    public static final String GEN_AI_SYSTEM = "gen_ai.system";
    public static final String GEN_AI_REQUEST_MODEL = "gen_ai.request.model";
    public static final String GEN_AI_REQUEST_MAX_TOKENS = "gen_ai.request.max_tokens";
    public static final String GEN_AI_REQUEST_TEMPERATURE = "gen_ai.request.temperature";
    public static final String GEN_AI_REQUEST_TOP_P = "gen_ai.request.top_p";

    // GenAI response attributes
    public static final String GEN_AI_RESPONSE_FINISH_REASON = "gen_ai.response.finish_reasons";
    public static final String GEN_AI_RESPONSE_MODEL = "gen_ai.response.model";

    // GenAI usage attributes
    public static final String GEN_AI_USAGE_INPUT_TOKENS = "gen_ai.usage.input_tokens";
    public static final String GEN_AI_USAGE_OUTPUT_TOKENS = "gen_ai.usage.output_tokens";

    // Tool attributes
    public static final String TOOL_NAME = "tool.name";
    public static final String TOOL_SUCCESS = "tool.success";
    public static final String TOOL_DURATION_MS = "tool.duration_ms";

    // Agent attributes
    public static final String AGENT_NAME = "agent.name";
    public static final String AGENT_ID = "agent.id";

    // Event loop attributes
    public static final String EVENT_LOOP_CYCLE = "event_loop.cycle";
    public static final String EVENT_LOOP_STOP_REASON = "event_loop.stop_reason";

    // MCP attributes
    public static final String MCP_SERVER_NAME = "mcp.server.name";
    public static final String MCP_TRANSPORT = "mcp.transport";
    public static final String MCP_TOOL_NAME = "mcp.tool.name";

    // Error attributes
    public static final String ERROR = "error";
    public static final String ERROR_TYPE = "error.type";
    public static final String ERROR_MESSAGE = "error.message";

    // Strands system value
    public static final String SYSTEM_VALUE = "strands-agents";
}
