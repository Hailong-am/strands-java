package com.strands.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HookRegistry {

    private static final Logger log = LoggerFactory.getLogger(HookRegistry.class);
    private final Map<Class<? extends HookEvent>, List<HookHandler<?>>> handlers = new ConcurrentHashMap<>();
    private final Set<Class<? extends HookEvent>> reverseOrderEvents = ConcurrentHashMap.newKeySet();

    public <E extends HookEvent> void register(Class<E> eventType, HookHandler<E> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    public void setReverseOrder(Class<? extends HookEvent> eventType) {
        reverseOrderEvents.add(eventType);
    }

    @SuppressWarnings("unchecked")
    public <E extends HookEvent> void emit(E event) {
        List<HookHandler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers == null) {
            return;
        }

        List<HookHandler<?>> ordered = eventHandlers;
        if (reverseOrderEvents.contains(event.getClass())) {
            ordered = new ArrayList<>(eventHandlers);
            Collections.reverse(ordered);
        }

        for (HookHandler<?> handler : ordered) {
            if (event.isConsumed()) {
                break;
            }
            try {
                ((HookHandler<E>) handler).handle(event);
            } catch (Exception e) {
                log.error("Hook handler failed for event {}", event.getClass().getSimpleName(), e);
            }
        }
    }

    public void registerProvider(HookProvider provider) {
        provider.registerHooks(this);
    }
}
