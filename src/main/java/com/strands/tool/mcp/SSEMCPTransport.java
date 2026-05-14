package com.strands.tool.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SSEMCPTransport implements MCPTransport {

    private static final Logger log = LoggerFactory.getLogger(SSEMCPTransport.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final Map<String, String> headers;

    private String messagesEndpoint;
    private ExecutorService sseExecutor;
    private final AtomicInteger requestId = new AtomicInteger(1);
    private final Map<Integer, CompletableFuture<Map<String, Object>>> pendingRequests = new ConcurrentHashMap<>();
    private volatile boolean connected;

    public SSEMCPTransport(String baseUrl) {
        this(baseUrl, null);
    }

    public SSEMCPTransport(String baseUrl, Map<String, String> headers) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.headers = headers != null ? headers : Map.of();
    }

    @Override
    public void connect() {
        sseExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "mcp-sse-listener");
            t.setDaemon(true);
            return t;
        });

        CompletableFuture<String> endpointFuture = new CompletableFuture<>();

        sseExecutor.submit(() -> {
            try {
                HttpURLConnection conn = openSSEConnection(baseUrl + "/sse");
                int status = conn.getResponseCode();
                if (status >= 400) {
                    endpointFuture.completeExceptionally(
                            new RuntimeException("SSE connection failed with status " + status));
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    String eventType = null;

                    while ((line = reader.readLine()) != null && connected) {
                        if (line.startsWith("event: ")) {
                            eventType = line.substring(7).trim();
                        } else if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            handleSSEEvent(eventType, data, endpointFuture);
                            eventType = null;
                        }
                    }
                }
            } catch (Exception e) {
                if (connected) {
                    log.error("SSE connection error", e);
                    endpointFuture.completeExceptionally(e);
                }
            }
        });

        connected = true;

        try {
            messagesEndpoint = endpointFuture.get(30, TimeUnit.SECONDS);
            log.debug("SSE connected, messages endpoint: {}", messagesEndpoint);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timed out waiting for SSE endpoint");
        } catch (Exception e) {
            throw new RuntimeException("Failed to establish SSE connection", e);
        }
    }

    @Override
    public void disconnect() {
        connected = false;
        pendingRequests.values().forEach(f -> f.cancel(true));
        pendingRequests.clear();
        if (sseExecutor != null) {
            sseExecutor.shutdownNow();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> initialize() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", Map.of());
        params.put("clientInfo", Map.of("name", "strands-java", "version", "0.1.0"));

        Map<String, Object> response = sendRequest("initialize", params);
        Map<String, Object> result = (Map<String, Object>) response.get("result");

        sendNotification("notifications/initialized", Map.of());

        return result != null ? result : Map.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MCPToolDefinition> listTools() {
        Map<String, Object> response = sendRequest("tools/list", Map.of());
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        if (result == null) return List.of();

        List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
        if (tools == null) return List.of();

        List<MCPToolDefinition> definitions = new ArrayList<>();
        for (Map<String, Object> tool : tools) {
            String name = (String) tool.get("name");
            String description = (String) tool.get("description");
            Map<String, Object> inputSchema = (Map<String, Object>) tool.get("inputSchema");
            definitions.add(new MCPToolDefinition(name, description, inputSchema));
        }
        return definitions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> callTool(String name, Map<String, Object> arguments) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", name);
        params.put("arguments", arguments != null ? arguments : Map.of());

        Map<String, Object> response = sendRequest("tools/call", params);

        Map<String, Object> error = (Map<String, Object>) response.get("error");
        if (error != null) {
            throw new RuntimeException("MCP tool error: " + error.get("message"));
        }

        Map<String, Object> result = (Map<String, Object>) response.get("result");
        return result != null ? result : Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendRequest(String method, Map<String, Object> params) {
        if (messagesEndpoint == null) {
            throw new IllegalStateException("Not connected - no messages endpoint");
        }

        int id = requestId.getAndIncrement();
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        request.put("params", params);

        try {
            postJson(messagesEndpoint, request);
            return future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingRequests.remove(id);
            throw new RuntimeException("MCP request timed out: " + method);
        } catch (Exception e) {
            pendingRequests.remove(id);
            throw new RuntimeException("MCP request failed: " + method, e);
        }
    }

    private void sendNotification(String method, Map<String, Object> params) {
        if (messagesEndpoint == null) return;

        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("jsonrpc", "2.0");
        notification.put("method", method);
        notification.put("params", params);

        try {
            postJson(messagesEndpoint, notification);
        } catch (Exception e) {
            log.warn("Failed to send notification: {}", method, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSSEEvent(String eventType, String data, CompletableFuture<String> endpointFuture) {
        if ("endpoint".equals(eventType)) {
            String endpoint = data.startsWith("http") ? data : baseUrl + data;
            endpointFuture.complete(endpoint);
        } else if ("message".equals(eventType)) {
            try {
                Map<String, Object> message = MAPPER.readValue(data, Map.class);
                Object id = message.get("id");
                if (id != null) {
                    int responseId = ((Number) id).intValue();
                    CompletableFuture<Map<String, Object>> pending = pendingRequests.remove(responseId);
                    if (pending != null) {
                        pending.complete(message);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse SSE message", e);
            }
        }
    }

    private void postJson(String url, Map<String, Object> body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        headers.forEach(conn::setRequestProperty);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(MAPPER.writeValueAsBytes(body));
        }

        int status = conn.getResponseCode();
        if (status >= 400) {
            throw new IOException("POST to " + url + " failed with status " + status);
        }
        conn.disconnect();
    }

    private HttpURLConnection openSSEConnection(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setRequestProperty("Cache-Control", "no-cache");
        headers.forEach(conn::setRequestProperty);
        conn.setDoInput(true);
        return conn;
    }
}
