package com.strands.model;

import com.strands.types.streaming.StreamEvent;

import java.util.Iterator;
import java.util.Map;

public interface Model {

    Iterator<StreamEvent> stream(StreamRequest request);

    ModelConfig getConfig();

    void updateConfig(Map<String, Object> config);
}
