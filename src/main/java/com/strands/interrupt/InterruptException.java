package com.strands.interrupt;

public class InterruptException extends RuntimeException {

    private final Interrupt interrupt;

    public InterruptException(Interrupt interrupt) {
        super("Agent interrupted: " + interrupt.getName());
        this.interrupt = interrupt;
    }

    public Interrupt getInterrupt() {
        return interrupt;
    }
}
