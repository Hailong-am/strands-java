package com.strands.plugin.steering;

import com.strands.agent.Agent;
import com.strands.hook.HookRegistry;
import com.strands.hook.events.AfterModelCallEvent;
import com.strands.hook.events.BeforeInvocationEvent;
import com.strands.hook.events.BeforeToolCallEvent;
import com.strands.plugin.Plugin;
import com.strands.types.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Steering plugin that evaluates handlers at key decision points:
 * - Before each tool call (steerBeforeTool)
 * - After each model response (steerAfterModel)
 * - Before each invocation (populates context via providers)
 *
 * Actions: Proceed (continue), Guide (inject guidance and retry), Interrupt (cancel).
 */
public class SteeringPlugin extends Plugin {

    private final List<SteeringHandler> handlers = new ArrayList<>();
    private final List<SteeringContextProvider> providers = new ArrayList<>();
    private final SteeringContext context = new SteeringContext();
    private final LedgerProvider ledger;
    private Agent agent;

    public SteeringPlugin(SteeringHandler... handlers) {
        this(null, handlers);
    }

    public SteeringPlugin(LedgerProvider ledger, SteeringHandler... handlers) {
        this.ledger = ledger != null ? ledger : new LedgerProvider();
        this.handlers.addAll(List.of(handlers));
    }

    public SteeringPlugin addHandler(SteeringHandler handler) {
        this.handlers.add(handler);
        return this;
    }

    public SteeringPlugin addProvider(SteeringContextProvider provider) {
        this.providers.add(provider);
        return this;
    }

    public SteeringContext getContext() {
        return context;
    }

    public LedgerProvider getLedger() {
        return ledger;
    }

    @Override
    public void initAgent(Agent agent) {
        this.agent = agent;
    }

    @Override
    public void registerHooks(HookRegistry registry) {
        registry.register(BeforeInvocationEvent.class, this::onBeforeInvocation);
        registry.register(BeforeToolCallEvent.class, this::onBeforeToolCall);
        registry.register(AfterModelCallEvent.class, this::onAfterModelCall);
    }

    private void onBeforeInvocation(BeforeInvocationEvent event) {
        if (agent == null) return;

        SteeringContextProvider.SteeringInput input = SteeringContextProvider.SteeringInput.forInvocation(
                event.getPrompt(),
                agent.getSystemPrompt(),
                agent.getState().toMap(),
                agent.getMessages()
        );

        for (SteeringContextProvider provider : providers) {
            provider.populate(context, input);
        }
    }

    private void onBeforeToolCall(BeforeToolCallEvent event) {
        if (handlers.isEmpty() || agent == null) return;

        SteeringContextProvider.SteeringInput input = SteeringContextProvider.SteeringInput.forTool(
                event.getToolName(),
                event.getToolUse().getInput(),
                agent.getMessages()
        );

        for (SteeringContextProvider provider : providers) {
            provider.populate(context, input);
        }

        for (SteeringHandler handler : handlers) {
            SteeringAction action = handler.evaluate(context, input);
            String handlerName = handler.getClass().getSimpleName();
            ledger.record("before_tool", handlerName, action, context.getVersion());

            if (action instanceof SteeringAction.Guide g) {
                event.setCancelled(true);
                injectGuidance(g.guidance());
            } else if (action instanceof SteeringAction.Interrupt i) {
                event.setCancelled(true);
                event.setConsumed(true);
                context.set("_interrupted", true);
                context.set("_interrupt_reason", i.reason());
            }

            if (event.isCancelled()) break;
        }
    }

    private void onAfterModelCall(AfterModelCallEvent event) {
        if (handlers.isEmpty() || agent == null) return;

        SteeringContextProvider.SteeringInput input = SteeringContextProvider.SteeringInput.forModelResponse(
                agent.getMessages()
        );

        for (SteeringContextProvider provider : providers) {
            provider.populate(context, input);
        }

        for (SteeringHandler handler : handlers) {
            SteeringAction action = handler.evaluate(context, input);
            String handlerName = handler.getClass().getSimpleName();
            ledger.record("after_model", handlerName, action, context.getVersion());

            if (action instanceof SteeringAction.Guide g) {
                injectGuidance(g.guidance());
                event.setRetry(true);
            } else if (action instanceof SteeringAction.Interrupt i) {
                event.setConsumed(true);
                context.set("_interrupted", true);
                context.set("_interrupt_reason", i.reason());
            }

            if (event.isConsumed()) break;
        }
    }

    private void injectGuidance(String guidance) {
        Message guidanceMessage = Message.user("[Steering Guidance]: " + guidance);
        agent.addMessage(guidanceMessage);
    }
}
