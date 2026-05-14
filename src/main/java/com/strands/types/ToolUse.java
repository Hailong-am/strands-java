package com.strands.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolUse {

    private String toolUseId;
    private String name;
    private Map<String, Object> input;

    public ToolUse() {
    }

    public ToolUse(String toolUseId, String name, Map<String, Object> input) {
        this.toolUseId = toolUseId;
        this.name = name;
        this.input = input;
    }

    @JsonProperty("toolUseId")
    public String getToolUseId() {
        return toolUseId;
    }

    public void setToolUseId(String toolUseId) {
        this.toolUseId = toolUseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }
}
