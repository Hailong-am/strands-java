package com.strands.tool;

import com.strands.types.ToolResult;
import com.strands.types.ToolResultContent;
import com.strands.types.ToolSpec;
import com.strands.types.ToolUse;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnnotatedMethodTool implements AgentTool {

    private final Object target;
    private final Method method;
    private final String toolName;
    private final ToolSpec toolSpec;

    public AnnotatedMethodTool(Object target, Method method) {
        this.target = target;
        this.method = method;
        this.method.setAccessible(true);

        Tool annotation = method.getAnnotation(Tool.class);
        this.toolName = annotation.name().isEmpty() ? method.getName() : annotation.name();
        this.toolSpec = buildToolSpec(annotation);
    }

    private ToolSpec buildToolSpec(Tool annotation) {
        String description = annotation.description().isEmpty()
                ? "Tool: " + toolName
                : annotation.description();

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Parameter param : method.getParameters()) {
            if (ToolContext.class.isAssignableFrom(param.getType())) {
                continue;
            }

            Param paramAnnotation = param.getAnnotation(Param.class);
            String paramName = paramAnnotation != null ? paramAnnotation.value() : param.getName();
            String paramDesc = paramAnnotation != null ? paramAnnotation.description() : "";
            boolean isRequired = paramAnnotation == null || paramAnnotation.required();

            Map<String, Object> propSchema = new LinkedHashMap<>();
            propSchema.put("type", jsonType(param.getType()));
            if (!paramDesc.isEmpty()) {
                propSchema.put("description", paramDesc);
            }
            properties.put(paramName, propSchema);

            if (isRequired) {
                required.add(paramName);
            }
        }

        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", properties);
        if (!required.isEmpty()) {
            inputSchema.put("required", required);
        }

        return new ToolSpec(toolName, description, inputSchema);
    }

    private String jsonType(Class<?> type) {
        if (type == String.class) return "string";
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) return "integer";
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) return "number";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        if (List.class.isAssignableFrom(type)) return "array";
        if (Map.class.isAssignableFrom(type)) return "object";
        return "string";
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
            Object[] args = resolveArguments(toolUse, context);
            Object result = method.invoke(target, args);
            String resultText = result != null ? result.toString() : "";
            return new ToolResult(toolUse.getToolUseId(), ToolResult.Status.SUCCESS,
                    List.of(ToolResultContent.fromText(resultText)));
        } catch (Exception e) {
            return new ToolResult(toolUse.getToolUseId(), ToolResult.Status.ERROR,
                    List.of(ToolResultContent.fromText(e.getMessage())));
        }
    }

    private Object[] resolveArguments(ToolUse toolUse, ToolContext context) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        Map<String, Object> input = toolUse.getInput() != null ? toolUse.getInput() : Map.of();

        for (int i = 0; i < params.length; i++) {
            if (ToolContext.class.isAssignableFrom(params[i].getType())) {
                args[i] = context;
                continue;
            }

            Param paramAnnotation = params[i].getAnnotation(Param.class);
            String paramName = paramAnnotation != null ? paramAnnotation.value() : params[i].getName();
            Object value = input.get(paramName);
            args[i] = coerce(value, params[i].getType());
        }
        return args;
    }

    private Object coerce(Object value, Class<?> targetType) {
        if (value == null) return defaultValue(targetType);
        if (targetType.isInstance(value)) return value;
        if (targetType == String.class) return value.toString();
        if (targetType == int.class || targetType == Integer.class) return ((Number) value).intValue();
        if (targetType == long.class || targetType == Long.class) return ((Number) value).longValue();
        if (targetType == double.class || targetType == Double.class) return ((Number) value).doubleValue();
        if (targetType == float.class || targetType == Float.class) return ((Number) value).floatValue();
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value.toString());
        return value;
    }

    private Object defaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == boolean.class) return false;
        return null;
    }
}
