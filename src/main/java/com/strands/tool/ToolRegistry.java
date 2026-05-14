package com.strands.tool;

import com.strands.types.ToolSpec;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ToolRegistry {

    private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();

    public void register(AgentTool tool) {
        tools.put(tool.getToolName(), tool);
    }

    public void registerAll(Object toolProvider) {
        for (Method method : toolProvider.getClass().getMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {
                AnnotatedMethodTool tool = new AnnotatedMethodTool(toolProvider, method);
                register(tool);
            }
        }
    }

    public AgentTool get(String name) {
        return tools.get(name);
    }

    public boolean contains(String name) {
        return tools.containsKey(name);
    }

    public void remove(String name) {
        tools.remove(name);
    }

    public List<ToolSpec> getToolSpecs() {
        List<ToolSpec> specs = new ArrayList<>();
        for (AgentTool tool : tools.values()) {
            specs.add(tool.getToolSpec());
        }
        return Collections.unmodifiableList(specs);
    }

    public Map<String, AgentTool> getAll() {
        return Collections.unmodifiableMap(tools);
    }

    public int size() {
        return tools.size();
    }
}
