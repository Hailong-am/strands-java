package com.strands.tool;

import java.util.List;

public interface ToolProvider {

    List<AgentTool> loadTools();

    void addConsumer(String consumerId);

    void removeConsumer(String consumerId);
}
