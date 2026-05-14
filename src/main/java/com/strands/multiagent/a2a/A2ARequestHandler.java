package com.strands.multiagent.a2a;

import com.strands.agent.AgentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles incoming A2A JSON-RPC requests. Dispatches to the appropriate
 * agent and manages task lifecycle.
 */
public class A2ARequestHandler {

    private static final Logger log = LoggerFactory.getLogger(A2ARequestHandler.class);

    private final A2AServer server;
    private final A2ATaskStore taskStore;
    private final Map<String, CompletableFuture<AgentResult>> runningTasks = new ConcurrentHashMap<>();

    public A2ARequestHandler(A2AServer server) {
        this(server, new A2ATaskStore());
    }

    public A2ARequestHandler(A2AServer server, A2ATaskStore taskStore) {
        this.server = server;
        this.taskStore = taskStore;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> handleRequest(Map<String, Object> request) {
        String method = (String) request.get("method");
        Object id = request.get("id");
        Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", Map.of());

        Map<String, Object> result;
        try {
            result = switch (method) {
                case "tasks/send" -> handleTaskSend(params);
                case "tasks/get" -> handleTaskGet(params);
                case "tasks/cancel" -> handleTaskCancel(params);
                case "agent/cards" -> handleAgentCards();
                default -> Map.of("error", Map.of("code", -32601, "message", "Method not found: " + method));
            };
        } catch (Exception e) {
            result = Map.of("error", Map.of("code", -32603, "message", e.getMessage()));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        if (result.containsKey("error")) {
            response.put("error", result.get("error"));
        } else {
            response.put("result", result);
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> handleTaskSend(Map<String, Object> params) {
        String agentName = (String) params.get("agent");
        Map<String, Object> message = (Map<String, Object>) params.get("message");
        String prompt = "";
        if (message != null) {
            List<Map<String, Object>> parts = (List<Map<String, Object>>) message.get("parts");
            if (parts != null && !parts.isEmpty()) {
                prompt = (String) parts.get(0).getOrDefault("text", "");
            }
        }

        A2AAgent a2aAgent = server.getAgent(agentName);
        if (a2aAgent == null) {
            return Map.of("error", Map.of("code", -32602, "message", "Agent not found: " + agentName));
        }

        A2ATask task = taskStore.create(agentName, prompt);
        task.setStatus(A2ATask.Status.WORKING);

        String finalPrompt = prompt;
        CompletableFuture<AgentResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                AgentResult result = a2aAgent.invoke(finalPrompt);
                task.setOutput(result.toString());
                task.setStatus(A2ATask.Status.COMPLETED);
                return result;
            } catch (Exception e) {
                task.setStatus(A2ATask.Status.FAILED);
                task.setOutput("Error: " + e.getMessage());
                throw e;
            } finally {
                runningTasks.remove(task.getId());
            }
        });
        runningTasks.put(task.getId(), future);

        return task.toMap();
    }

    private Map<String, Object> handleTaskGet(Map<String, Object> params) {
        String taskId = (String) params.get("id");
        return taskStore.get(taskId)
                .map(A2ATask::toMap)
                .orElse(Map.of("error", Map.of("code", -32602, "message", "Task not found: " + taskId)));
    }

    private Map<String, Object> handleTaskCancel(Map<String, Object> params) {
        String taskId = (String) params.get("id");
        CompletableFuture<AgentResult> future = runningTasks.get(taskId);
        if (future != null) {
            future.cancel(true);
        }
        boolean cancelled = taskStore.cancel(taskId);
        return Map.of("cancelled", cancelled);
    }

    private Map<String, Object> handleAgentCards() {
        return server.getAgentCards();
    }

    public A2ATaskStore getTaskStore() {
        return taskStore;
    }
}
