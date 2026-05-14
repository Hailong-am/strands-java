package com.strands.tool.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class StdioMCPTransport implements MCPTransport {

    private static final Logger log = LoggerFactory.getLogger(StdioMCPTransport.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<String> command;
    private final Map<String, String> env;
    private final File workingDirectory;

    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private final AtomicInteger requestId = new AtomicInteger(1);

    public StdioMCPTransport(List<String> command) {
        this(command, null, null);
    }

    public StdioMCPTransport(List<String> command, Map<String, String> env, File workingDirectory) {
        this.command = command;
        this.env = env;
        this.workingDirectory = workingDirectory;
    }

    @Override
    public void connect() {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            if (workingDirectory != null) {
                pb.directory(workingDirectory);
            }
            if (env != null) {
                pb.environment().putAll(env);
            }
            pb.redirectErrorStream(false);

            process = pb.start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            log.debug("Started MCP server process: {}", String.join(" ", command));
        } catch (IOException e) {
            throw new RuntimeException("Failed to start MCP server process", e);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            log.warn("Error closing streams", e);
        }
        if (process != null) {
            process.destroyForcibly();
            log.debug("Destroyed MCP server process");
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
    private synchronized Map<String, Object> sendRequest(String method, Map<String, Object> params) {
        int id = requestId.getAndIncrement();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        request.put("params", params);

        try {
            String json = MAPPER.writeValueAsString(request);
            writer.write(json);
            writer.newLine();
            writer.flush();

            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    throw new RuntimeException("MCP server process terminated unexpectedly");
                }
                if (line.isBlank()) continue;

                Map<String, Object> response = MAPPER.readValue(line, Map.class);
                Object responseId = response.get("id");
                if (responseId != null && ((Number) responseId).intValue() == id) {
                    return response;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to communicate with MCP server", e);
        }
    }

    private synchronized void sendNotification(String method, Map<String, Object> params) {
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("jsonrpc", "2.0");
        notification.put("method", method);
        notification.put("params", params);

        try {
            String json = MAPPER.writeValueAsString(notification);
            writer.write(json);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            log.warn("Failed to send notification: {}", method, e);
        }
    }
}
