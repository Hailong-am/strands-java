package com.strands.tool.mcp;

import java.util.Map;

/**
 * Represents a long-running MCP task. When a tool call returns a task handle
 * instead of an immediate result, this object tracks the task until completion.
 */
public class MCPTask {

    public enum Status {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    private final String taskId;
    private final String toolName;
    private Status status;
    private Object result;
    private String error;
    private Map<String, Object> metadata;

    public MCPTask(String taskId, String toolName) {
        this.taskId = taskId;
        this.toolName = toolName;
        this.status = Status.PENDING;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getToolName() {
        return toolName;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
        this.status = Status.COMPLETED;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
        this.status = Status.FAILED;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public boolean isTerminal() {
        return status == Status.COMPLETED || status == Status.FAILED || status == Status.CANCELLED;
    }
}
