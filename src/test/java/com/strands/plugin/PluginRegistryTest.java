package com.strands.plugin;

import com.strands.agent.Agent;
import com.strands.hook.HookRegistry;
import com.strands.hook.events.MessageAddedEvent;
import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.tool.AgentTool;
import com.strands.tool.Param;
import com.strands.tool.Tool;
import com.strands.tool.ToolRegistry;
import com.strands.types.streaming.StreamEvent;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class PluginRegistryTest {

    @Test
    void testPluginToolsRegistered() {
        ToolRegistry toolRegistry = new ToolRegistry();
        HookRegistry hookRegistry = new HookRegistry();
        PluginRegistry pluginRegistry = new PluginRegistry();

        pluginRegistry.register(new TestPlugin());
        pluginRegistry.initializeAll(buildAgent(), toolRegistry, hookRegistry);

        assertTrue(toolRegistry.contains("plugin_tool"));
    }

    @Test
    void testPluginHooksRegistered() {
        ToolRegistry toolRegistry = new ToolRegistry();
        HookRegistry hookRegistry = new HookRegistry();
        PluginRegistry pluginRegistry = new PluginRegistry();

        TestPlugin plugin = new TestPlugin();
        pluginRegistry.register(plugin);
        pluginRegistry.initializeAll(buildAgent(), toolRegistry, hookRegistry);

        hookRegistry.emit(new MessageAddedEvent(com.strands.types.Message.user("hello")));
        assertTrue(plugin.hookCalled.get());
    }

    @Test
    void testPluginInitAgentCalled() {
        ToolRegistry toolRegistry = new ToolRegistry();
        HookRegistry hookRegistry = new HookRegistry();
        PluginRegistry pluginRegistry = new PluginRegistry();

        TestPlugin plugin = new TestPlugin();
        pluginRegistry.register(plugin);
        pluginRegistry.initializeAll(buildAgent(), toolRegistry, hookRegistry);

        assertTrue(plugin.initCalled.get());
    }

    @Test
    void testMultiplePlugins() {
        ToolRegistry toolRegistry = new ToolRegistry();
        HookRegistry hookRegistry = new HookRegistry();
        PluginRegistry pluginRegistry = new PluginRegistry();

        pluginRegistry.register(new TestPlugin());
        pluginRegistry.register(new AnotherPlugin());
        pluginRegistry.initializeAll(buildAgent(), toolRegistry, hookRegistry);

        assertTrue(toolRegistry.contains("plugin_tool"));
        assertTrue(toolRegistry.contains("another_tool"));
        assertEquals(2, pluginRegistry.getAll().size());
    }

    private Agent buildAgent() {
        return Agent.builder()
                .model(new Model() {
                    @Override
                    public Iterator<StreamEvent> stream(StreamRequest request) {
                        return List.of(
                                StreamEvent.messageStart("assistant"),
                                StreamEvent.contentBlockDelta(0, Map.of("text", "ok")),
                                StreamEvent.messageStop("end_turn"),
                                StreamEvent.metadata(1, 1, 10)
                        ).iterator();
                    }

                    @Override
                    public ModelConfig getConfig() {
                        return new ModelConfig("mock");
                    }

                    @Override
                    public void updateConfig(Map<String, Object> cfg) {
                    }
                })
                .build();
    }

    static class TestPlugin extends Plugin {
        AtomicBoolean initCalled = new AtomicBoolean(false);
        AtomicBoolean hookCalled = new AtomicBoolean(false);

        @Override
        public void initAgent(Agent agent) {
            initCalled.set(true);
        }

        @Tool(name = "plugin_tool", description = "A plugin tool")
        public String doSomething(@Param(value = "input", description = "input") String input) {
            return "result";
        }

        @Hook
        public void onMessage(MessageAddedEvent event) {
            hookCalled.set(true);
        }
    }

    static class AnotherPlugin extends Plugin {
        @Tool(name = "another_tool", description = "Another tool")
        public String another(@Param(value = "x", description = "x") String x) {
            return x;
        }
    }
}
