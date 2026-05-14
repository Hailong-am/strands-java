package com.strands.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HookRegistry {

    private static final Logger log = LoggerFactory.getLogger(HookRegistry.class);
    private final Map<Class<? extends HookEvent>, List<HookHandler<?>>> handlers = new ConcurrentHashMap<>();

    public <E extends HookEvent> void register(Class<E> eventType, HookHandler<E> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    @SuppressWarnings("unchecked")
    public <E extends HookEvent> void emit(E event) {
        List<HookHandler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers == null) {
            return;
        }

        for (HookHandler<?> handler : eventHandlers) {
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
