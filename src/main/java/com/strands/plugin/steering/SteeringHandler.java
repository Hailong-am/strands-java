package com.strands.plugin.steering;

/**
 * Evaluates the steering context and returns an action directing the agent.
 * Handlers are invoked at key decision points: before tool calls and after model responses.
 */
public interface SteeringHandler {

    SteeringAction evaluate(SteeringContext context, SteeringContextProvider.SteeringInput input);
}
