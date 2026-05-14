package com.strands.event;

import com.strands.hook.HookRegistry;
import com.strands.hook.events.AfterModelCallEvent;
import com.strands.hook.events.AfterToolCallEvent;
import com.strands.hook.events.BeforeModelCallEvent;
import com.strands.hook.events.BeforeToolCallEvent;
import com.strands.model.*;
import com.strands.tool.AgentTool;
import com.strands.tool.ToolExecutor;
import com.strands.tool.ToolRegistry;
import com.strands.types.*;
import com.strands.types.streaming.StreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EventLoop {

    private static final Logger log = LoggerFactory.getLogger(EventLoop.class);
    private static final int MAX_CYCLES = 100;

    private final Model model;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final HookRegistry hookRegistry;
    private final StreamProcessor streamProcessor;
    private final String systemPrompt;

    public EventLoop(Model model, ToolRegistry toolRegistry, ToolExecutor toolExecutor,
                     HookRegistry hookRegistry, String systemPrompt) {
        this.model = model;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.hookRegistry = hookRegistry;
        this.streamProcessor = new StreamProcessor();
        this.systemPrompt = systemPrompt;
    }

    public EventLoopResult run(List<Message> messages, InvocationState state, StreamHandler handler) {
        int cycleCount = 0;

        while (cycleCount < MAX_CYCLES) {
            cycleCount++;
            state.getMetrics().incrementCycleCount();

            BeforeModelCallEvent beforeEvent = new BeforeModelCallEvent(messages);
            hookRegistry.emit(beforeEvent);

            StreamRequest request = StreamRequest.builder()
                    .messages(messages)
                    .toolSpecs(toolRegistry.getToolSpecs())
                    .systemPrompt(systemPrompt)
                    .build();

            Iterator<StreamEvent> stream = model.stream(request);
            StreamResult result = streamProcessor.process(stream, handler);

            state.getMetrics().accumulateUsage(result.getUsage());
            state.getMetrics().setLatencyMs(state.getMetrics().getLatencyMs() + result.getLatencyMs());

            AfterModelCallEvent afterEvent = new AfterModelCallEvent(result.getMessage(), result.getStopReason());
            hookRegistry.emit(afterEvent);

            if (afterEvent.isRetry()) {
                log.debug("Retry requested by hook, repeating cycle");
                continue;
            }

            messages.add(result.getMessage());

            if (result.getStopReason() == StopReason.MAX_TOKENS) {
                Message recovered = recoverPartialMessage(result.getMessage());
                if (recovered != null && recovered.hasToolUse()) {
                    log.debug("Recovered partial tool use from max_tokens response");
                    List<ToolUse> toolUses = recovered.getToolUses();
                    List<ToolResult> toolResults = executeTools(toolUses, state);
                    List<ContentBlock> resultBlocks = new ArrayList<>();
                    for (ToolResult toolResult : toolResults) {
                        resultBlocks.add(ContentBlock.fromToolResult(toolResult));
                    }
                    messages.add(new Message(Message.Role.USER, resultBlocks));
                    continue;
                }
                return new EventLoopResult(result.getMessage(), result.getStopReason(), state.getMetrics(), state.getProperties());
            }

            if (result.getStopReason() == StopReason.TOOL_USE) {
                List<ToolUse> toolUses = result.getMessage().getToolUses();
                List<ToolResult> toolResults = executeTools(toolUses, state);

                List<ContentBlock> resultBlocks = new ArrayList<>();
                for (ToolResult toolResult : toolResults) {
                    resultBlocks.add(ContentBlock.fromToolResult(toolResult));
                }
                Message toolResultMessage = new Message(Message.Role.USER, resultBlocks);
                messages.add(toolResultMessage);

                continue;
            }

            return new EventLoopResult(result.getMessage(), result.getStopReason(), state.getMetrics(), state.getProperties());
        }

        log.warn("Event loop exceeded max cycles ({})", MAX_CYCLES);
        Message lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        return new EventLoopResult(lastMessage, StopReason.MAX_TOKENS, state.getMetrics(), state.getProperties());
    }

    public Iterator<StreamEvent> streamEvents(List<Message> messages, InvocationState state) {
        StreamRequest request = StreamRequest.builder()
                .messages(messages)
                .toolSpecs(toolRegistry.getToolSpecs())
                .systemPrompt(systemPrompt)
                .build();
        return model.stream(request);
    }

    private List<ToolResult> executeTools(List<ToolUse> toolUses, InvocationState state) {
        List<ToolResult> results = new ArrayList<>();

        for (ToolUse toolUse : toolUses) {
            AgentTool tool = toolRegistry.get(toolUse.getName());

            BeforeToolCallEvent beforeEvent = new BeforeToolCallEvent(toolUse, tool);
            hookRegistry.emit(beforeEvent);

            if (beforeEvent.isCancelled()) {
                results.add(ToolResult.error(toolUse.getToolUseId(), "Tool call cancelled by hook"));
                continue;
            }

            AgentTool effectiveTool = beforeEvent.getSelectedTool();
            ToolResult result;
            if (effectiveTool == null) {
                result = ToolResult.error(toolUse.getToolUseId(), "Tool not found: " + toolUse.getName());
            } else {
                result = toolExecutor.execute(List.of(toolUse), toolRegistry, state.getProperties()).get(0);
            }

            AfterToolCallEvent afterEvent = new AfterToolCallEvent(toolUse, result);
            hookRegistry.emit(afterEvent);

            results.add(afterEvent.getResult());
        }

        return results;
    }

    private Message recoverPartialMessage(Message message) {
        if (message == null) return null;
        List<ContentBlock> content = message.getContent();
        if (content == null || content.isEmpty()) return null;

        List<ContentBlock> validBlocks = new ArrayList<>();
        for (ContentBlock block : content) {
            if (block.isToolUse()) {
                ToolUse tu = block.getToolUse();
                if (tu.getName() != null && tu.getToolUseId() != null
                        && toolRegistry.contains(tu.getName())) {
                    if (tu.getInput() == null) {
                        tu.setInput(java.util.Map.of());
                    }
                    validBlocks.add(block);
                }
            } else if (block.isText()) {
                validBlocks.add(block);
            }
        }

        if (validBlocks.isEmpty()) return null;
        return new Message(message.getRole(), validBlocks);
    }
}
