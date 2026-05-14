package com.strands.tool.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP transport using HTTP streaming (Streamable HTTP).
 * Implements the MCP HTTP transport spec where each JSON-RPC request
 * is sent as an HTTP POST and the response may be streamed via SSE.
 */
public class HttpStreamMCPTransport implements MCPTransport {

    private static final Logger log = LoggerFactory.getLogger(HttpStreamMCPTransport.class);

    private final String endpoint;
    private final Map<String, String> headers;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicLong requestId = new AtomicLong(1);
    private String sessionId;

    public HttpStreamMCPTransport(String endpoint) {
        this(endpoint, Map.of());
    }

    public HttpStreamMCPTransport(String endpoint, Map<String, String> headers) {
        this.endpoint = endpoint;
        this.headers = new HashMap<>(headers);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void connect() {
        log.debug("HTTP stream transport connecting to: {}", endpoint);
    }

    @Override
    public void disconnect() {
        log.debug("HTTP stream transport disconnecting from: {}", endpoint);
        sessionId = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> initialize() {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "method", "initialize",
                "id", nextId(),
                "params", Map.of(
                        "protocolVersion", "2024-11-05",
                        "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "strands-java", "version", "1.0.0")
                )
        );

        Map<String, Object> response = sendRaw(request);
        if (response.containsKey("result")) {
            return (Map<String, Object>) response.get("result");
        }
        return response;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MCPToolDefinition> listTools() {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/list",
                "id", nextId()
        );

        Map<String, Object> response = sendRaw(request);
        Map<String, Object> result = response.containsKey("result")
                ? (Map<String, Object>) response.get("result")
                : response;

        List<Map<String, Object>> toolsRaw = (List<Map<String, Object>>) result.getOrDefault("tools", List.of());
        List<MCPToolDefinition> tools = new ArrayList<>();
        for (Map<String, Object> toolMap : toolsRaw) {
            tools.add(new MCPToolDefinition(
                    (String) toolMap.get("name"),
                    (String) toolMap.get("description"),
                    (Map<String, Object>) toolMap.getOrDefault("inputSchema", Map.of())
            ));
        }
        return tools;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> callTool(String name, Map<String, Object> arguments) {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/call",
                "id", nextId(),
                "params", Map.of("name", name, "arguments", arguments)
        );

        Map<String, Object> response = sendRaw(request);
        if (response.containsKey("result")) {
            return (Map<String, Object>) response.get("result");
        }
        return response;
    }

    @Override
    public Map<String, Object> sendRaw(Map<String, Object> request) {
        try {
            String body = objectMapper.writeValueAsString(request);

            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(60));

            headers.forEach(httpRequestBuilder::header);
            if (sessionId != null) {
                httpRequestBuilder.header("Mcp-Session-Id", sessionId);
            }

            HttpResponse<String> response = httpClient.send(
                    httpRequestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            response.headers().firstValue("Mcp-Session-Id").ifPresent(id -> sessionId = id);

            String contentType = response.headers().firstValue("Content-Type").orElse("");
            String responseBody = response.body();

            if (contentType.contains("text/event-stream")) {
                return parseSSEResponse(responseBody);
            }

            return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("HTTP request failed", e);
            return Map.of("error", Map.of("message", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSSEResponse(String sseBody) {
        Map<String, Object> lastData = new HashMap<>();
        for (String line : sseBody.split("\n")) {
            if (line.startsWith("data: ")) {
                try {
                    lastData = objectMapper.readValue(
                            line.substring(6), new TypeReference<Map<String, Object>>() {});
                } catch (Exception ignored) {
                }
            }
        }
        return lastData;
    }

    private long nextId() {
        return requestId.getAndIncrement();
    }
}
