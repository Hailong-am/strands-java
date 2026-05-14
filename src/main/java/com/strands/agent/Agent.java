package com.strands.agent;

import com.strands.event.EventLoop;
import com.strands.event.EventLoopResult;
import com.strands.event.InvocationState;
import com.strands.hook.HookProvider;
import com.strands.hook.HookRegistry;
import com.strands.hook.events.AfterInvocationEvent;
import com.strands.hook.events.AgentInitializedEvent;
import com.strands.hook.events.MessageAddedEvent;
import com.strands.model.CacheConfig;
import com.strands.model.Model;
import com.strands.model.StreamHandler;
import com.strands.plugin.Plugin;
import com.strands.plugin.PluginRegistry;
import com.strands.tool.*;
import com.strands.tool.structured.SchemaGenerator;
import com.strands.tool.structured.StructuredOutputTool;
import com.strands.types.ContentBlock;
import com.strands.types.Message;
import com.strands.types.exceptions.ConcurrencyException;
import com.strands.types.exceptions.StructuredOutputException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class Agent {

    private final String agentId;
    private final String name;
    private final Model model;
    private String systemPrompt;
    private final List<Message> messages;
    private final AgentState state;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final HookRegistry hookRegistry;
    private final ConversationManager conversationManager;
    private final ConcurrentInvocationMode concurrentInvocationMode;
    private final CacheConfig cacheConfig;
    private final PluginRegistry pluginRegistry;
    private final AtomicBoolean invoking = new AtomicBoolean(false);

    private Agent(Builder builder) {
        this.agentId = builder.agentId != null ? builder.agentId : UUID.randomUUID().toString();
        this.name = builder.name;
        this.model = builder.model;
        this.systemPrompt = builder.systemPrompt;
        this.messages = new ArrayList<>();
        this.state = new AgentState();
        this.toolRegistry = new ToolRegistry();
        this.toolExecutor = builder.toolExecutor != null ? builder.toolExecutor : new ConcurrentToolExecutor();
        this.hookRegistry = new HookRegistry();
        this.conversationManager = builder.conversationManager != null
                ? builder.conversationManager
                : new SlidingWindowConversationManager();
        this.concurrentInvocationMode = builder.concurrentInvocationMode != null
                ? builder.concurrentInvocationMode
                : ConcurrentInvocationMode.THROW;
        this.cacheConfig = builder.cacheConfig;
        this.pluginRegistry = new PluginRegistry();

        registerTools(builder.tools);
        registerToolProviders(builder.toolProviders);
        registerHookProviders(builder.hookProviders);
        registerPlugins(builder.plugins);
        hookRegistry.registerProvider(conversationManager);

        hookRegistry.emit(new AgentInitializedEvent(this));
    }

    public static Builder builder() {
        return new Builder();
    }

    public AgentResult invoke(String prompt) {
        return invoke(prompt, null);
    }

    public AgentResult invoke(String prompt, StreamHandler handler) {
        acquireInvocationLock();
        try {
            Message userMessage = Message.user(prompt);
            addMessage(userMessage);

            InvocationState invocationState = new InvocationState();
            EventLoop eventLoop = new EventLoop(model, toolRegistry, toolExecutor, hookRegistry, systemPrompt);

            EventLoopResult result = eventLoop.run(messages, invocationState, handler);

            conversationManager.applyManagement(this);

            AgentResult agentResult = new AgentResult(result.getStopReason(), result.getMessage(),
                    result.getMetrics(), state);

            AfterInvocationEvent afterEvent = new AfterInvocationEvent(this);
            hookRegistry.emit(afterEvent);

            if (afterEvent.isResume() && afterEvent.getResumeInput() != null) {
                releaseInvocationLock();
                return invoke(afterEvent.getResumeInput(), handler);
            }

            return agentResult;
        } finally {
            releaseInvocationLock();
        }
    }

    public CompletableFuture<AgentResult> invokeAsync(String prompt) {
        return invokeAsync(prompt, null);
    }

    public CompletableFuture<AgentResult> invokeAsync(String prompt, StreamHandler handler) {
        return CompletableFuture.supplyAsync(() -> invoke(prompt, handler));
    }

    @SuppressWarnings("unchecked")
    public <T> T structuredOutput(Class<T> outputType, String prompt) {
        Map<String, Object> schema = SchemaGenerator.generateSchema(outputType);
        String toolName = "structured_output_" + outputType.getSimpleName().toLowerCase();
        StructuredOutputTool outputTool = new StructuredOutputTool(
                outputType, toolName,
                "Return the final result as a structured " + outputType.getSimpleName(),
                schema);

        toolRegistry.register(outputTool);
        try {
            Message userMessage = Message.user(prompt);
            addMessage(userMessage);

            InvocationState invocationState = new InvocationState();
            EventLoop eventLoop = new EventLoop(model, toolRegistry, toolExecutor, hookRegistry, systemPrompt);

            EventLoopResult result = eventLoop.run(messages, invocationState, null);
            conversationManager.applyManagement(this);

            Object output = result.getInvocationProperties().get("_structured_output");
            if (output == null) {
                throw new StructuredOutputException(
                        "Model did not produce structured output. Stop reason: " + result.getStopReason());
            }
            return (T) output;
        } finally {
            toolRegistry.remove(toolName);
        }
    }

    public <T> CompletableFuture<T> structuredOutputAsync(Class<T> outputType, String prompt) {
        return CompletableFuture.supplyAsync(() -> structuredOutput(outputType, prompt));
    }

    public AgentTool asTool(String toolName, String description) {
        return com.strands.multiagent.AgentAsTool.wrap(this, toolName, description);
    }

    public void addMessage(Message message) {
        messages.add(message);
        hookRegistry.emit(new MessageAddedEvent(message));
    }

    private void acquireInvocationLock() {
        if (!invoking.compareAndSet(false, true)) {
            if (concurrentInvocationMode == ConcurrentInvocationMode.THROW) {
                throw new ConcurrencyException("Agent is already processing an invocation. "
                        + "Use ConcurrentInvocationMode.UNSAFE_REENTRANT to allow concurrent calls.");
            }
        }
    }

    private void releaseInvocationLock() {
        invoking.set(false);
    }

    public String getAgentId() {
        return agentId;
    }

    public String getName() {
        return name;
    }

    public Model getModel() {
        return model;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public AgentState getState() {
        return state;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public HookRegistry getHookRegistry() {
        return hookRegistry;
    }

    public ConversationManager getConversationManager() {
        return conversationManager;
    }

    public CacheConfig getCacheConfig() {
        return cacheConfig;
    }

    private void registerTools(List<AgentTool> tools) {
        if (tools != null) {
            for (AgentTool tool : tools) {
                toolRegistry.register(tool);
            }
        }
    }

    private void registerToolProviders(List<Object> providers) {
        if (providers != null) {
            for (Object provider : providers) {
                toolRegistry.registerAll(provider);
            }
        }
    }

    private void registerHookProviders(List<HookProvider> providers) {
        if (providers != null) {
            for (HookProvider provider : providers) {
                hookRegistry.registerProvider(provider);
            }
        }
    }

    private void registerPlugins(List<Plugin> plugins) {
        if (plugins != null) {
            for (Plugin plugin : plugins) {
                pluginRegistry.register(plugin);
            }
            pluginRegistry.initializeAll(this, toolRegistry, hookRegistry);
        }
    }

    public static class Builder {
        private String agentId;
        private String name;
        private Model model;
        private String systemPrompt;
        private List<AgentTool> tools;
        private List<Object> toolProviders;
        private List<HookProvider> hookProviders;
        private List<Plugin> plugins;
        private ToolExecutor toolExecutor;
        private ConversationManager conversationManager;
        private ConcurrentInvocationMode concurrentInvocationMode;
        private CacheConfig cacheConfig;

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder model(Model model) {
            this.model = model;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder tools(AgentTool... tools) {
            this.tools = List.of(tools);
            return this;
        }

        public Builder tools(List<AgentTool> tools) {
            this.tools = tools;
            return this;
        }

        public Builder toolProviders(Object... providers) {
            this.toolProviders = List.of(providers);
            return this;
        }

        public Builder hookProviders(HookProvider... providers) {
            this.hookProviders = List.of(providers);
            return this;
        }

        public Builder plugins(Plugin... plugins) {
            this.plugins = List.of(plugins);
            return this;
        }

        public Builder plugins(List<Plugin> plugins) {
            this.plugins = plugins;
            return this;
        }

        public Builder toolExecutor(ToolExecutor toolExecutor) {
            this.toolExecutor = toolExecutor;
            return this;
        }

        public Builder conversationManager(ConversationManager conversationManager) {
            this.conversationManager = conversationManager;
            return this;
        }

        public Builder concurrentInvocationMode(ConcurrentInvocationMode mode) {
            this.concurrentInvocationMode = mode;
            return this;
        }

        public Builder cacheConfig(CacheConfig cacheConfig) {
            this.cacheConfig = cacheConfig;
            return this;
        }

        public Agent build() {
            if (model == null) {
                throw new IllegalStateException("Model is required");
            }
            return new Agent(this);
        }
    }
}
