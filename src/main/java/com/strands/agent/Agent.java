package com.strands.agent;

import com.strands.event.EventLoop;
import com.strands.event.EventLoopResult;
import com.strands.event.InvocationState;
import com.strands.hook.HookProvider;
import com.strands.hook.HookRegistry;
import com.strands.hook.events.AfterInvocationEvent;
import com.strands.hook.events.AgentInitializedEvent;
import com.strands.hook.events.MessageAddedEvent;
import com.strands.model.Model;
import com.strands.model.StreamHandler;
import com.strands.tool.*;
import com.strands.types.ContentBlock;
import com.strands.types.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Agent {

    private final String agentId;
    private final String name;
    private final Model model;
    private final String systemPrompt;
    private final List<Message> messages;
    private final AgentState state;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final HookRegistry hookRegistry;
    private final ConversationManager conversationManager;

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

        registerTools(builder.tools);
        registerToolProviders(builder.toolProviders);
        registerHookProviders(builder.hookProviders);
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
            return invoke(afterEvent.getResumeInput(), handler);
        }

        return agentResult;
    }

    public void addMessage(Message message) {
        messages.add(message);
        hookRegistry.emit(new MessageAddedEvent(message));
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

    public static class Builder {
        private String agentId;
        private String name;
        private Model model;
        private String systemPrompt;
        private List<AgentTool> tools;
        private List<Object> toolProviders;
        private List<HookProvider> hookProviders;
        private ToolExecutor toolExecutor;
        private ConversationManager conversationManager;

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

        public Builder toolExecutor(ToolExecutor toolExecutor) {
            this.toolExecutor = toolExecutor;
            return this;
        }

        public Builder conversationManager(ConversationManager conversationManager) {
            this.conversationManager = conversationManager;
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
