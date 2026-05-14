package com.strands.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ToolLoader {

    private static final Logger log = LoggerFactory.getLogger(ToolLoader.class);

    public static List<AgentTool> loadFromProviders(List<ToolProvider> providers) {
        List<AgentTool> tools = new ArrayList<>();
        for (ToolProvider provider : providers) {
            try {
                List<AgentTool> providerTools = provider.loadTools();
                tools.addAll(providerTools);
                log.debug("Loaded {} tools from provider {}", providerTools.size(), provider.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("Failed to load tools from provider {}", provider.getClass().getSimpleName(), e);
            }
        }
        return tools;
    }

    public static List<AgentTool> loadFromObjects(List<Object> objects) {
        List<AgentTool> tools = new ArrayList<>();
        for (Object obj : objects) {
            if (obj instanceof AgentTool tool) {
                tools.add(tool);
            } else if (obj instanceof ToolProvider provider) {
                tools.addAll(provider.loadTools());
            } else {
                ToolRegistry temp = new ToolRegistry();
                temp.registerAll(obj);
                tools.addAll(temp.getAll().values());
            }
        }
        return tools;
    }
}
