package com.strands.model;

import com.strands.types.Message;
import com.strands.types.ToolSpec;

import java.util.List;
import java.util.Map;

public class StreamRequest {

    private final List<Message> messages;
    private final List<ToolSpec> toolSpecs;
    private final String systemPrompt;
    private final Map<String, Object> modelConfig;

    private StreamRequest(Builder builder) {
        this.messages = builder.messages;
        this.toolSpecs = builder.toolSpecs;
        this.systemPrompt = builder.systemPrompt;
        this.modelConfig = builder.modelConfig;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public List<ToolSpec> getToolSpecs() {
        return toolSpecs;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public Map<String, Object> getModelConfig() {
        return modelConfig;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Message> messages = List.of();
        private List<ToolSpec> toolSpecs = List.of();
        private String systemPrompt;
        private Map<String, Object> modelConfig = Map.of();

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public Builder toolSpecs(List<ToolSpec> toolSpecs) {
            this.toolSpecs = toolSpecs;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder modelConfig(Map<String, Object> modelConfig) {
            this.modelConfig = modelConfig;
            return this;
        }

        public StreamRequest build() {
            return new StreamRequest(this);
        }
    }
}
