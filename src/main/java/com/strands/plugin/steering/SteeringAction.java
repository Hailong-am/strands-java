package com.strands.plugin.steering;

/**
 * Action returned by a SteeringHandler indicating how the agent should proceed.
 */
public sealed interface SteeringAction {

    record Proceed() implements SteeringAction {}

    record Guide(String guidance) implements SteeringAction {}

    record Interrupt(String reason) implements SteeringAction {}

    static SteeringAction proceed() { return new Proceed(); }
    static SteeringAction guide(String guidance) { return new Guide(guidance); }
    static SteeringAction interrupt(String reason) { return new Interrupt(reason); }
}
