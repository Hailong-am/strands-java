package com.strands.plugin;

import com.strands.agent.Agent;
import com.strands.hook.HookEvent;
import com.strands.hook.HookRegistry;
import com.strands.tool.AgentTool;
import com.strands.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PluginRegistry {

    private static final Logger log = LoggerFactory.getLogger(PluginRegistry.class);
    private final List<Plugin> plugins = new ArrayList<>();

    public void register(Plugin plugin) {
        plugins.add(plugin);
    }

    public void initializeAll(Agent agent, ToolRegistry toolRegistry, HookRegistry hookRegistry) {
        for (Plugin plugin : plugins) {
            plugin.initAgent(agent);

            List<AgentTool> tools = plugin.discoverTools();
            for (AgentTool tool : tools) {
                toolRegistry.register(tool);
            }

            plugin.registerHooks(hookRegistry);
            discoverHookMethods(plugin, hookRegistry);
        }
    }

    @SuppressWarnings("unchecked")
    private void discoverHookMethods(Plugin plugin, HookRegistry hookRegistry) {
        for (Method method : plugin.getClass().getMethods()) {
            if (method.isAnnotationPresent(Hook.class)) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 1 && HookEvent.class.isAssignableFrom(paramTypes[0])) {
                    Class<? extends HookEvent> eventType = (Class<? extends HookEvent>) paramTypes[0];
                    hookRegistry.register(eventType, event -> {
                        try {
                            method.invoke(plugin, event);
                        } catch (Exception e) {
                            log.error("Plugin hook method failed: {}.{}",
                                    plugin.getClass().getSimpleName(), method.getName(), e);
                        }
                    });
                }
            }
        }
    }

    public List<Plugin> getAll() {
        return plugins;
    }
}
