package com.strands.plugin.steering;

import com.strands.agent.Agent;
import com.strands.hook.HookRegistry;
import com.strands.hook.events.BeforeInvocationEvent;
import com.strands.plugin.Hook;
import com.strands.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Steering plugin that injects just-in-time contextual guidance into the
 * system prompt before each invocation. Supports multiple SteeringHandlers
 * that can provide guidance based on conversation state.
 */
public class SteeringPlugin extends Plugin {

    private final List<SteeringHandler> handlers = new ArrayList<>();
    private Agent agent;

    public SteeringPlugin(SteeringHandler... handlers) {
        this.handlers.addAll(List.of(handlers));
    }

    public SteeringPlugin addHandler(SteeringHandler handler) {
        this.handlers.add(handler);
        return this;
    }

    @Override
    public void initAgent(Agent agent) {
        this.agent = agent;
    }

    @Hook
    public void onBeforeInvocation(BeforeInvocationEvent event) {
        if (handlers.isEmpty() || agent == null) return;

        SteeringHandler.SteeringContext context = new SteeringHandler.SteeringContext(
                event.getPrompt(),
                agent.getSystemPrompt(),
                agent.getState().toMap(),
                agent.getMessages().size()
        );

        StringBuilder guidance = new StringBuilder();
        for (SteeringHandler handler : handlers) {
            String g = handler.getGuidance(context);
            if (g != null && !g.isBlank()) {
                guidance.append("\n\n").append(g);
            }
        }

        if (guidance.length() > 0) {
            String currentPrompt = agent.getSystemPrompt();
            String augmented = currentPrompt != null
                    ? currentPrompt + guidance
                    : guidance.toString().trim();
            agent.setSystemPrompt(augmented);
        }
    }

    @Override
    public void registerHooks(HookRegistry registry) {
    }
}
