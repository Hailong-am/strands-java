package com.strands.tool.structured;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strands.tool.AgentTool;
import com.strands.tool.ToolContext;
import com.strands.types.*;
import com.strands.types.exceptions.StructuredOutputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class StructuredOutputTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(StructuredOutputTool.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String name;
    private final ToolSpec toolSpec;
    private final Class<?> outputType;

    public StructuredOutputTool(Class<?> outputType, String name, String description, Map<String, Object> schema) {
        this.outputType = outputType;
        this.name = name;

        String fullDescription = "IMPORTANT: This StructuredOutputTool should only be invoked as the last and final tool "
                + "before returning the completed result to the caller. " + description;
        this.toolSpec = new ToolSpec(name, fullDescription, schema);
    }

    @Override
    public String getToolName() {
        return name;
    }

    @Override
    public ToolSpec getToolSpec() {
        return toolSpec;
    }

    @Override
    public ToolResult invoke(ToolUse toolUse, ToolContext context) {
        Map<String, Object> input = toolUse.getInput();
        try {
            String json = MAPPER.writeValueAsString(input);
            Object validated = MAPPER.readValue(json, outputType);
            context.setState("_structured_output", validated);
            return ToolResult.success(toolUse.getToolUseId(), "Structured output validated successfully");
        } catch (Exception e) {
            log.warn("Structured output validation failed: {}", e.getMessage());
            return ToolResult.error(toolUse.getToolUseId(), "Validation error: " + e.getMessage());
        }
    }

    public Class<?> getOutputType() {
        return outputType;
    }
}
