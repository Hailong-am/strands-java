package com.strands.model;

import com.strands.types.Message;
import com.strands.types.ToolSpec;
import com.strands.types.streaming.StreamEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface Model {

    Iterator<StreamEvent> stream(StreamRequest request);

    ModelConfig getConfig();

    void updateConfig(Map<String, Object> config);

    default int countTokens(List<Message> messages, List<ToolSpec> toolSpecs) {
        int estimate = 0;
        for (Message msg : messages) {
            estimate += msg.getTextContent().length() / 4;
        }
        for (ToolSpec spec : toolSpecs) {
            estimate += spec.getDescription().length() / 4;
        }
        return estimate;
    }

    default int getContextWindowLimit() {
        return ModelContextWindows.getLimit(getConfig().getModelId());
    }
}
