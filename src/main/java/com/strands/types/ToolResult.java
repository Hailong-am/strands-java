package com.strands.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResult {

    public enum Status {
        SUCCESS("success"),
        ERROR("error");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private String toolUseId;
    private Status status;
    private List<ToolResultContent> content;

    public ToolResult() {
    }

    public ToolResult(String toolUseId, Status status, List<ToolResultContent> content) {
        this.toolUseId = toolUseId;
        this.status = status;
        this.content = content;
    }

    public static ToolResult success(String toolUseId, String text) {
        return new ToolResult(toolUseId, Status.SUCCESS, List.of(ToolResultContent.fromText(text)));
    }

    public static ToolResult error(String toolUseId, String errorMessage) {
        return new ToolResult(toolUseId, Status.ERROR, List.of(ToolResultContent.fromText(errorMessage)));
    }

    @JsonProperty("toolUseId")
    public String getToolUseId() {
        return toolUseId;
    }

    public void setToolUseId(String toolUseId) {
        this.toolUseId = toolUseId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<ToolResultContent> getContent() {
        return content;
    }

    public void setContent(List<ToolResultContent> content) {
        this.content = content;
    }
}
