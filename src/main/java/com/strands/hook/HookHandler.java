package com.strands.hook;

@FunctionalInterface
public interface HookHandler<E extends HookEvent> {

    void handle(E event);
}
