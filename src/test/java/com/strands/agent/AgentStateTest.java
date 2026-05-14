package com.strands.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentStateTest {

    private AgentState state;

    @BeforeEach
    void setUp() {
        state = new AgentState();
    }

    @Test
    void testSetAndGet() {
        state.set("key", "value");
        assertEquals("value", state.get("key"));
    }

    @Test
    void testGetTyped() {
        state.set("count", 42);
        assertEquals(42, state.get("count", Integer.class));
    }

    @Test
    void testHas() {
        assertFalse(state.has("missing"));
        state.set("present", true);
        assertTrue(state.has("present"));
    }

    @Test
    void testRemove() {
        state.set("key", "val");
        state.remove("key");
        assertFalse(state.has("key"));
        assertNull(state.get("key"));
    }

    @Test
    void testToMap() {
        state.set("a", 1);
        state.set("b", "two");
        Map<String, Object> map = state.toMap();
        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
        assertEquals("two", map.get("b"));
    }

    @Test
    void testToMapIsUnmodifiable() {
        state.set("x", 1);
        Map<String, Object> map = state.toMap();
        assertThrows(UnsupportedOperationException.class, () -> map.put("y", 2));
    }

    @Test
    void testLoadFrom() {
        state.set("old", "data");
        state.loadFrom(Map.of("new1", "a", "new2", "b"));
        assertFalse(state.has("old"));
        assertEquals("a", state.get("new1"));
        assertEquals("b", state.get("new2"));
    }

    @Test
    void testLoadFromNull() {
        state.set("key", "val");
        state.loadFrom(null);
        assertFalse(state.has("key"));
    }
}
