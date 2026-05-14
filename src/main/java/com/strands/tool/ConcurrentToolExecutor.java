package com.strands.tool;

import com.strands.types.ToolResult;
import com.strands.types.ToolResultContent;
import com.strands.types.ToolUse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrentToolExecutor implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentToolExecutor.class);
    private final ExecutorService executorService;

    public ConcurrentToolExecutor() {
        this(Executors.newCachedThreadPool());
    }

    public ConcurrentToolExecutor(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public List<ToolResult> execute(List<ToolUse> toolUses, ToolRegistry registry, Map<String, Object> invocationState) {
        if (toolUses.size() == 1) {
            return List.of(executeSingle(toolUses.get(0), registry, invocationState));
        }

        List<CompletableFuture<ToolResult>> futures = new ArrayList<>();
        for (ToolUse toolUse : toolUses) {
            futures.add(CompletableFuture.supplyAsync(
                    () -> executeSingle(toolUse, registry, invocationState),
                    executorService));
        }

        List<ToolResult> results = new ArrayList<>();
        for (CompletableFuture<ToolResult> future : futures) {
            results.add(future.join());
        }
        return results;
    }

    private ToolResult executeSingle(ToolUse toolUse, ToolRegistry registry, Map<String, Object> invocationState) {
        AgentTool tool = registry.get(toolUse.getName());
        if (tool == null) {
            log.warn("Tool not found: {}", toolUse.getName());
            return new ToolResult(toolUse.getToolUseId(), ToolResult.Status.ERROR,
                    List.of(ToolResultContent.fromText("Tool not found: " + toolUse.getName())));
        }

        ToolContext context = new ToolContext(toolUse, invocationState);
        try {
            return tool.invoke(toolUse, context);
        } catch (Exception e) {
            log.error("Tool execution failed: {}", toolUse.getName(), e);
            return new ToolResult(toolUse.getToolUseId(), ToolResult.Status.ERROR,
                    List.of(ToolResultContent.fromText("Tool execution error: " + e.getMessage())));
        }
    }
}
