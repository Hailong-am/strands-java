package com.strands.multiagent;

import com.strands.agent.Agent;
import com.strands.agent.AgentResult;
import com.strands.tool.AgentTool;
import com.strands.tool.ToolContext;
import com.strands.types.ToolResult;
import com.strands.types.ToolResultContent;
import com.strands.types.ToolSpec;
import com.strands.types.ToolUse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AgentAsTool implements AgentTool {

    private final Agent agent;
    private final String name;
    private final String description;
    private final boolean preserveContext;

    private AgentAsTool(Agent agent, String name, String description, boolean preserveContext) {
        this.agent = agent;
        this.name = name;
        this.description = description;
        this.preserveContext = preserveContext;
    }

    public static AgentAsTool wrap(Agent agent, String name, String description) {
        return new AgentAsTool(agent, name, description, true);
    }

    public static AgentAsTool wrap(Agent agent, String name, String description, boolean preserveContext) {
        return new AgentAsTool(agent, name, description, preserveContext);
    }

    @Override
    public String getToolName() {
        return name;
    }

    @Override
    public ToolSpec getToolSpec() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("prompt", Map.of(
                "type", "string",
                "description", "The prompt/instruction to send to the agent"
        ));

        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", properties);
        inputSchema.put("required", List.of("prompt"));

        return new ToolSpec(name, description, inputSchema);
    }

    @Override
    public ToolResult invoke(ToolUse toolUse, ToolContext context) {
        String prompt = (String) toolUse.getInput().get("prompt");

        if (!preserveContext) {
            agent.getMessages().clear();
        }

        try {
            AgentResult result = agent.invoke(prompt);
            String responseText = result.toString();
            return new ToolResult(toolUse.getToolUseId(), ToolResult.Status.SUCCESS,
                    List.of(ToolResultContent.fromText(responseText)));
        } catch (Exception e) {
            return new ToolResult(toolUse.getToolUseId(), ToolResult.Status.ERROR,
                    List.of(ToolResultContent.fromText("Agent invocation failed: " + e.getMessage())));
        }
    }
}
