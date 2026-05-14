package com.strands.plugin.offloader;

import com.strands.hook.HookRegistry;
import com.strands.hook.events.AfterToolCallEvent;
import com.strands.plugin.Hook;
import com.strands.plugin.Plugin;
import com.strands.tool.Param;
import com.strands.tool.Tool;
import com.strands.types.ToolResult;
import com.strands.types.ToolResultContent;

import java.util.List;
import java.util.UUID;

public class ContextOffloaderPlugin extends Plugin {

    private final OffloaderStorage storage;
    private final int thresholdChars;
    private final int previewChars;

    public ContextOffloaderPlugin() {
        this(new InMemoryStorage(), 10000, 500);
    }

    public ContextOffloaderPlugin(OffloaderStorage storage, int thresholdChars, int previewChars) {
        this.storage = storage;
        this.thresholdChars = thresholdChars;
        this.previewChars = previewChars;
    }

    @Hook
    public void onAfterToolCall(AfterToolCallEvent event) {
        ToolResult result = event.getResult();
        if (result == null || result.getContent() == null) return;

        int totalLength = 0;
        for (ToolResultContent c : result.getContent()) {
            if (c.getText() != null) {
                totalLength += c.getText().length();
            }
        }

        if (totalLength > thresholdChars) {
            StringBuilder fullContent = new StringBuilder();
            for (ToolResultContent c : result.getContent()) {
                if (c.getText() != null) fullContent.append(c.getText());
            }

            String key = "offloaded_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            storage.store(key, fullContent.toString());

            String preview = fullContent.substring(0, Math.min(previewChars, fullContent.length()));
            String truncatedText = preview + "\n\n[Content truncated. "
                    + totalLength + " chars total. Use retrieve_offloaded tool with key='" + key + "' to get full content.]";

            ToolResult truncated = new ToolResult(
                    result.getToolUseId(),
                    result.getStatus(),
                    List.of(ToolResultContent.fromText(truncatedText))
            );
            event.setResult(truncated);
        }
    }

    @Tool(name = "retrieve_offloaded", description = "Retrieves previously offloaded tool result content by key")
    public String retrieveOffloaded(
            @Param(value = "key", description = "The offload key from the truncation message") String key) {
        return storage.retrieve(key).orElse("Content not found for key: " + key);
    }

    @Override
    public void registerHooks(HookRegistry registry) {
        // @Hook annotation handles registration via PluginRegistry.discoverHookMethods
    }
}
