package com.strands.hook.events;

import com.strands.hook.HookEvent;
import com.strands.tool.AgentTool;
import com.strands.types.ToolUse;

public class BeforeToolCallEvent extends HookEvent {

    private final ToolUse toolUse;
    private AgentTool selectedTool;
    private boolean cancelled;

    public BeforeToolCallEvent(ToolUse toolUse, AgentTool selectedTool) {
        this.toolUse = toolUse;
        this.selectedTool = selectedTool;
    }

    public ToolUse getToolUse() {
        return toolUse;
    }

    public String getToolName() {
        return toolUse.getName();
    }

    public AgentTool getSelectedTool() {
        return selectedTool;
    }

    public void setSelectedTool(AgentTool selectedTool) {
        this.selectedTool = selectedTool;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
