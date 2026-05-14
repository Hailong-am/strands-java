package com.strands.hook.events;

import com.strands.hook.HookEvent;
import com.strands.types.ToolResult;
import com.strands.types.ToolUse;

public class AfterToolCallEvent extends HookEvent {

    private final ToolUse toolUse;
    private final ToolResult result;
    private boolean retry;

    public AfterToolCallEvent(ToolUse toolUse, ToolResult result) {
        this.toolUse = toolUse;
        this.result = result;
    }

    public ToolUse getToolUse() {
        return toolUse;
    }

    public String getToolName() {
        return toolUse.getName();
    }

    public ToolResult getResult() {
        return result;
    }

    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }
}
