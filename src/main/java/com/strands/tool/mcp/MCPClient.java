package com.strands.tool.mcp;

import com.strands.tool.AgentTool;
import com.strands.tool.ToolProvider;
import com.strands.types.exceptions.MCPClientInitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class MCPClient implements ToolProvider, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MCPClient.class);

    private final MCPTransport transport;
    private final long startupTimeoutMs;
    private final List<Predicate<String>> allowedFilters;
    private final List<Predicate<String>> rejectedFilters;
    private final String prefix;
    private final Set<String> consumers = ConcurrentHashMap.newKeySet();

    private ExecutorService backgroundExecutor;
    private volatile boolean connected;
    private String serverInstructions;
    private List<MCPToolDefinition> serverTools;

    public MCPClient(MCPTransport transport) {
        this(transport, 30000, null, null, null);
    }

    public MCPClient(MCPTransport transport, long startupTimeoutMs,
                     List<Predicate<String>> allowedFilters,
                     List<Predicate<String>> rejectedFilters,
                     String prefix) {
        this.transport = transport;
        this.startupTimeoutMs = startupTimeoutMs;
        this.allowedFilters = allowedFilters != null ? allowedFilters : List.of();
        this.rejectedFilters = rejectedFilters != null ? rejectedFilters : List.of();
        this.prefix = prefix;
    }

    public MCPClient start() {
        backgroundExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "mcp-client-" + hashCode());
            t.setDaemon(true);
            return t;
        });

        CompletableFuture<Void> initFuture = new CompletableFuture<>();

        backgroundExecutor.submit(() -> {
            try {
                transport.connect();
                Map<String, Object> initResult = transport.initialize();
                serverInstructions = (String) initResult.get("instructions");
                connected = true;
                initFuture.complete(null);
            } catch (Exception e) {
                initFuture.completeExceptionally(e);
            }
        });

        try {
            initFuture.get(startupTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new MCPClientInitializationError("MCP server startup timed out after " + startupTimeoutMs + "ms");
        } catch (Exception e) {
            throw new MCPClientInitializationError("MCP server initialization failed", e.getCause() != null ? e.getCause() : e);
        }

        return this;
    }

    @Override
    public List<AgentTool> loadTools() {
        if (!connected) {
            throw new IllegalStateException("MCPClient not connected. Call start() first.");
        }

        try {
            CompletableFuture<List<MCPToolDefinition>> future = CompletableFuture.supplyAsync(
                    () -> transport.listTools(), backgroundExecutor);
            serverTools = future.get(startupTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to list MCP tools", e);
        }

        List<AgentTool> tools = new ArrayList<>();
        for (MCPToolDefinition toolDef : serverTools) {
            String name = toolDef.getName();

            if (!allowedFilters.isEmpty() && allowedFilters.stream().noneMatch(f -> f.test(name))) {
                continue;
            }
            if (rejectedFilters.stream().anyMatch(f -> f.test(name))) {
                continue;
            }

            String toolName = prefix != null ? prefix + name : name;
            tools.add(new MCPAgentTool(toolDef, this, toolName));
        }

        return tools;
    }

    @Override
    public void addConsumer(String consumerId) {
        consumers.add(consumerId);
    }

    @Override
    public void removeConsumer(String consumerId) {
        consumers.remove(consumerId);
        if (consumers.isEmpty()) {
            close();
        }
    }

    public Map<String, Object> callTool(String name, Map<String, Object> arguments) {
        if (!connected) {
            throw new IllegalStateException("MCPClient not connected");
        }

        try {
            CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(
                    () -> transport.callTool(name, arguments), backgroundExecutor);
            return future.get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("MCP tool call failed: " + name, e);
        }
    }

    @Override
    public void close() {
        connected = false;
        if (transport != null) {
            try {
                transport.disconnect();
            } catch (Exception e) {
                log.warn("Error disconnecting MCP transport", e);
            }
        }
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> sendRequest(Map<String, Object> request) {
        if (!connected) {
            throw new IllegalStateException("MCPClient not connected");
        }
        try {
            CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(
                    () -> transport.sendRaw(request), backgroundExecutor);
            return future.get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("MCP request failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listPrompts() {
        Map<String, Object> result = sendRequest(Map.of(
                "jsonrpc", "2.0",
                "method", "prompts/list",
                "id", UUID.randomUUID().toString()
        ));
        Object prompts = result.get("prompts");
        return prompts instanceof List ? (List<Map<String, Object>>) prompts : List.of();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getPrompt(String name, Map<String, String> arguments) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        if (arguments != null) {
            params.put("arguments", arguments);
        }
        return sendRequest(Map.of(
                "jsonrpc", "2.0",
                "method", "prompts/get",
                "id", UUID.randomUUID().toString(),
                "params", params
        ));
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listResources() {
        Map<String, Object> result = sendRequest(Map.of(
                "jsonrpc", "2.0",
                "method", "resources/list",
                "id", UUID.randomUUID().toString()
        ));
        Object resources = result.get("resources");
        return resources instanceof List ? (List<Map<String, Object>>) resources : List.of();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> readResource(String uri) {
        return sendRequest(Map.of(
                "jsonrpc", "2.0",
                "method", "resources/read",
                "id", UUID.randomUUID().toString(),
                "params", Map.of("uri", uri)
        ));
    }

    public String getServerInstructions() {
        return serverInstructions;
    }

    public boolean isConnected() {
        return connected;
    }
}
