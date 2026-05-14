package com.strands.tool.mcp;

import com.strands.tool.AgentTool;
import com.strands.tool.ToolContext;
import com.strands.types.*;

import java.util.List;
import java.util.Map;

public class MCPAgentTool implements AgentTool {

    private final MCPToolDefinition mcpTool;
    private final MCPClient mcpClient;
    private final String toolName;
    private final ToolSpec toolSpec;

    public MCPAgentTool(MCPToolDefinition mcpTool, MCPClient mcpClient, String toolName) {
        this.mcpTool = mcpTool;
        this.mcpClient = mcpClient;
        this.toolName = toolName;

        String description = mcpTool.getDescription() != null
                ? mcpTool.getDescription()
                : "Tool which performs " + mcpTool.getName();

        this.toolSpec = new ToolSpec(toolName, description, mcpTool.getInputSchema());
    }

    @Override
    public String getToolName() {
        return toolName;
    }

    @Override
    public ToolSpec getToolSpec() {
        return toolSpec;
    }

    @Override
    public ToolResult invoke(ToolUse toolUse, ToolContext context) {
        try {
            Map<String, Object> result = mcpClient.callTool(mcpTool.getName(), toolUse.getInput());

            boolean isError = Boolean.TRUE.equals(result.get("isError"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");

            StringBuilder text = new StringBuilder();
            if (content != null) {
                for (Map<String, Object> block : content) {
                    if ("text".equals(block.get("type"))) {
                        if (text.length() > 0) text.append("\n");
                        text.append(block.get("text"));
                    }
                }
            }

            ToolResult.Status status = isError ? ToolResult.Status.ERROR : ToolResult.Status.SUCCESS;
            return new ToolResult(toolUse.getToolUseId(), status,
                    List.of(ToolResultContent.fromText(text.toString())));
        } catch (Exception e) {
            return ToolResult.error(toolUse.getToolUseId(), "MCP tool call failed: " + e.getMessage());
        }
    }

    public MCPToolDefinition getMcpTool() {
        return mcpTool;
    }
}
