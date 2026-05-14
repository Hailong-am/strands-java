package com.strands.multiagent.a2a;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory store for A2A tasks. Tracks submitted, active, and completed tasks.
 */
public class A2ATaskStore {

    private final Map<String, A2ATask> tasks = new ConcurrentHashMap<>();

    public A2ATask create(String agentName, String input) {
        A2ATask task = new A2ATask(agentName, input);
        tasks.put(task.getId(), task);
        return task;
    }

    public Optional<A2ATask> get(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public List<A2ATask> listByAgent(String agentName) {
        return tasks.values().stream()
                .filter(t -> t.getAgentName().equals(agentName))
                .collect(Collectors.toList());
    }

    public List<A2ATask> listByStatus(A2ATask.Status status) {
        return tasks.values().stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    public boolean cancel(String taskId) {
        A2ATask task = tasks.get(taskId);
        if (task != null && task.getStatus() != A2ATask.Status.COMPLETED) {
            task.setStatus(A2ATask.Status.CANCELLED);
            return true;
        }
        return false;
    }

    public void remove(String taskId) {
        tasks.remove(taskId);
    }

    public int size() {
        return tasks.size();
    }
}
