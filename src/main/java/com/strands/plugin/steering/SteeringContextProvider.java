package com.strands.plugin.steering;

/**
 * Provides contextual data to the steering system. Implementations auto-register
 * on hook events to populate the SteeringContext with relevant information.
 */
public interface SteeringContextProvider {

    void populate(SteeringContext context, SteeringInput input);

    record SteeringInput(
            String prompt,
            String systemPrompt,
            java.util.Map<String, Object> agentState,
            java.util.List<com.strands.types.Message> messages,
            String toolName,
            java.util.Map<String, Object> toolInput,
            com.strands.types.ToolResult toolResult
    ) {
        public static SteeringInput forInvocation(String prompt, String systemPrompt,
                                                   java.util.Map<String, Object> agentState,
                                                   java.util.List<com.strands.types.Message> messages) {
            return new SteeringInput(prompt, systemPrompt, agentState, messages, null, null, null);
        }

        public static SteeringInput forTool(String toolName, java.util.Map<String, Object> toolInput,
                                             java.util.List<com.strands.types.Message> messages) {
            return new SteeringInput(null, null, null, messages, toolName, toolInput, null);
        }

        public static SteeringInput forModelResponse(java.util.List<com.strands.types.Message> messages) {
            return new SteeringInput(null, null, null, messages, null, null, null);
        }
    }
}
