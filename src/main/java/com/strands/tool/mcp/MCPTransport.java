package com.strands.tool.mcp;

import java.util.List;
import java.util.Map;

public interface MCPTransport {

    void connect();

    void disconnect();

    Map<String, Object> initialize();

    List<MCPToolDefinition> listTools();

    Map<String, Object> callTool(String name, Map<String, Object> arguments);

    default Map<String, Object> sendRaw(Map<String, Object> request) {
        throw new UnsupportedOperationException("Raw request not supported by this transport");
    }
}
