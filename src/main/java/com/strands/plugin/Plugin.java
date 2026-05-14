package com.strands.plugin;

import com.strands.agent.Agent;
import com.strands.hook.HookProvider;
import com.strands.hook.HookRegistry;
import com.strands.tool.AgentTool;
import com.strands.tool.AnnotatedMethodTool;
import com.strands.tool.Tool;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class Plugin implements HookProvider {

    public void initAgent(Agent agent) {
    }

    public List<AgentTool> discoverTools() {
        List<AgentTool> tools = new ArrayList<>();
        for (Method method : this.getClass().getMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {
                tools.add(new AnnotatedMethodTool(this, method));
            }
        }
        return tools;
    }

    @Override
    public void registerHooks(HookRegistry registry) {
    }
}
