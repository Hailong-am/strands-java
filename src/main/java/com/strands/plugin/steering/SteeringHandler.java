package com.strands.plugin.steering;

import java.util.Map;

/**
 * Provides just-in-time contextual guidance to the agent based on the
 * current conversation state.
 */
public interface SteeringHandler {

    /**
     * Returns guidance text to inject into the system prompt, or null if no guidance
     * is relevant for the current state.
     */
    String getGuidance(SteeringContext context);

    record SteeringContext(
            String currentPrompt,
            String systemPrompt,
            Map<String, Object> state,
            int messageCount
    ) {}
}
