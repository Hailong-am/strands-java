package com.strands.tool.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Manages long-running MCP tasks. Tracks task lifecycle from creation
 * to completion, supporting polling and async completion patterns.
 */
public class MCPTaskManager {

    private static final Logger log = LoggerFactory.getLogger(MCPTaskManager.class);

    private final Map<String, MCPTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Object>> futures = new ConcurrentHashMap<>();
    private final MCPClient client;

    public MCPTaskManager(MCPClient client) {
        this.client = client;
    }

    public MCPTask submitTask(String taskId, String toolName) {
        MCPTask task = new MCPTask(taskId, toolName);
        task.setStatus(MCPTask.Status.RUNNING);
        tasks.put(taskId, task);
        futures.put(taskId, new CompletableFuture<>());
        return task;
    }

    public Optional<MCPTask> getTask(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    @SuppressWarnings("unchecked")
    public MCPTask pollTask(String taskId) {
        MCPTask task = tasks.get(taskId);
        if (task == null || task.isTerminal()) return task;

        try {
            Map<String, Object> request = Map.of(
                    "jsonrpc", "2.0",
                    "id", System.currentTimeMillis(),
                    "method", "tasks/get",
                    "params", Map.of("taskId", taskId)
            );

            Map<String, Object> response = client.sendRequest(request);
            Map<String, Object> result = (Map<String, Object>) response.get("result");

            if (result != null) {
                String status = (String) result.get("status");
                if ("completed".equals(status)) {
                    task.setResult(result.get("result"));
                    completeFuture(taskId, result.get("result"));
                } else if ("failed".equals(status)) {
                    task.setError((String) result.getOrDefault("error", "Task failed"));
                    completeFuture(taskId, null);
                } else if ("cancelled".equals(status)) {
                    task.setStatus(MCPTask.Status.CANCELLED);
                    completeFuture(taskId, null);
                }
            }
        } catch (Exception e) {
            log.error("Failed to poll task {}: {}", taskId, e.getMessage());
        }

        return task;
    }

    public CompletableFuture<Object> awaitTask(String taskId) {
        return futures.getOrDefault(taskId, CompletableFuture.completedFuture(null));
    }

    public void cancelTask(String taskId) {
        MCPTask task = tasks.get(taskId);
        if (task != null && !task.isTerminal()) {
            task.setStatus(MCPTask.Status.CANCELLED);
            completeFuture(taskId, null);

            try {
                client.sendRequest(Map.of(
                        "jsonrpc", "2.0",
                        "id", System.currentTimeMillis(),
                        "method", "tasks/cancel",
                        "params", Map.of("taskId", taskId)
                ));
            } catch (Exception e) {
                log.warn("Failed to send cancel for task {}: {}", taskId, e.getMessage());
            }
        }
    }

    private void completeFuture(String taskId, Object result) {
        CompletableFuture<Object> future = futures.get(taskId);
        if (future != null && !future.isDone()) {
            future.complete(result);
        }
    }
}
