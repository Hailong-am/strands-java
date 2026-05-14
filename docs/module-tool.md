# Module: Tool System

## Overview

The tool system allows agents to invoke external capabilities. Tools are defined declaratively
and the framework handles schema generation, registration, invocation, and result formatting.

## Key Interfaces

### AgentTool
```java
public interface AgentTool {
    String getToolName();
    ToolSpec getToolSpec();
    ToolResult invoke(ToolUse toolUse, ToolContext context);
}
```

### @Tool Annotation
```java
@Tool(name = "calculator", description = "Performs arithmetic")
public String calculate(@Param("expression") String expression) {
    return eval(expression);
}
```

### ToolRegistry
- Stores tools by name
- Resolves tool references during execution
- Supports dynamic tool addition/removal

### ToolExecutor
- `ConcurrentToolExecutor` - runs tool calls from a single model response in parallel
- `SequentialToolExecutor` - runs one at a time (for tools with side effects)

### ToolContext
Injected into tool methods that declare it as a parameter:
- Access to current `ToolUse` (input, id)
- Access to the `Agent` instance
- Access to `InvocationState` (mutable shared context)

## Schema Generation

The `@Tool` annotation processor inspects method parameters to generate JSON Schema:
- `String` → `{"type": "string"}`
- `int/Integer` → `{"type": "integer"}`
- `boolean/Boolean` → `{"type": "boolean"}`
- `List<T>` → `{"type": "array", "items": ...}`
- `Map<String,T>` → `{"type": "object"}`
- `@Param` annotation provides name/description/required

## Execution Flow

1. Model returns `stopReason=tool_use` with one or more `ToolUse` blocks
2. EventLoop extracts tool uses from assistant message
3. ToolExecutor resolves each tool from registry
4. Fires `BeforeToolCallEvent` hook
5. Invokes `tool.invoke(toolUse, context)`
6. Fires `AfterToolCallEvent` hook
7. Packages results into `ToolResult` messages
8. Appends tool result message to conversation
9. Recurses event loop
