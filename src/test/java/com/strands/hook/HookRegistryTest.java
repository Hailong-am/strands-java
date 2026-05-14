package com.strands.hook;

import com.strands.hook.events.BeforeInvocationEvent;
import com.strands.hook.events.MessageAddedEvent;
import com.strands.types.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HookRegistryTest {

    private HookRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new HookRegistry();
    }

    @Test
    void testRegisterAndEmit() {
        AtomicInteger count = new AtomicInteger(0);
        registry.register(MessageAddedEvent.class, event -> count.incrementAndGet());

        registry.emit(new MessageAddedEvent(Message.user("hello")));

        assertEquals(1, count.get());
    }

    @Test
    void testMultipleHandlersForSameEvent() {
        List<String> calls = new ArrayList<>();
        registry.register(MessageAddedEvent.class, event -> calls.add("first"));
        registry.register(MessageAddedEvent.class, event -> calls.add("second"));

        registry.emit(new MessageAddedEvent(Message.user("test")));

        assertEquals(List.of("first", "second"), calls);
    }

    @Test
    void testConsumedEventStopsPropagation() {
        List<String> calls = new ArrayList<>();
        registry.register(MessageAddedEvent.class, event -> {
            calls.add("first");
            event.setConsumed(true);
        });
        registry.register(MessageAddedEvent.class, event -> calls.add("second"));

        registry.emit(new MessageAddedEvent(Message.user("test")));

        assertEquals(List.of("first"), calls);
    }

    @Test
    void testNoHandlersDoesNotThrow() {
        assertDoesNotThrow(() -> registry.emit(new MessageAddedEvent(Message.user("orphan"))));
    }

    @Test
    void testHandlerExceptionDoesNotPropagateOrStopOthers() {
        List<String> calls = new ArrayList<>();
        registry.register(MessageAddedEvent.class, event -> {
            throw new RuntimeException("boom");
        });
        registry.register(MessageAddedEvent.class, event -> calls.add("survived"));

        assertDoesNotThrow(() -> registry.emit(new MessageAddedEvent(Message.user("test"))));
        assertEquals(List.of("survived"), calls);
    }

    @Test
    void testBeforeInvocationEventPromptModification() {
        registry.register(BeforeInvocationEvent.class, event -> {
            event.setPrompt(event.getPrompt().toUpperCase());
        });

        BeforeInvocationEvent event = new BeforeInvocationEvent(null, "hello", new ArrayList<>());
        registry.emit(event);

        assertEquals("HELLO", event.getPrompt());
    }

    @Test
    void testRegisterProvider() {
        AtomicInteger count = new AtomicInteger(0);
        HookProvider provider = hookRegistry ->
                hookRegistry.register(MessageAddedEvent.class, event -> count.incrementAndGet());

        registry.registerProvider(provider);
        registry.emit(new MessageAddedEvent(Message.user("test")));

        assertEquals(1, count.get());
    }
}
