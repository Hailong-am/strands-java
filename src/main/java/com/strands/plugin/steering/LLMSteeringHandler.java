package com.strands.plugin.steering;

import com.strands.model.Model;
import com.strands.model.StreamRequest;
import com.strands.types.Message;
import com.strands.types.streaming.StreamEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Steering handler that delegates decisions to a separate LLM.
 * Sends the current context to the model with a decision prompt,
 * then parses the response into a SteeringAction.
 */
public class LLMSteeringHandler implements SteeringHandler {

    private final Model model;
    private final String decisionPrompt;

    public LLMSteeringHandler(Model model) {
        this(model, null);
    }

    public LLMSteeringHandler(Model model, String decisionPrompt) {
        this.model = model;
        this.decisionPrompt = decisionPrompt != null ? decisionPrompt : buildDefaultPrompt();
    }

    @Override
    public SteeringAction evaluate(SteeringContext context, SteeringContextProvider.SteeringInput input) {
        String prompt = buildPrompt(context, input);

        StreamRequest request = StreamRequest.builder()
                .messages(List.of(Message.user(prompt)))
                .systemPrompt(decisionPrompt)
                .build();

        StringBuilder response = new StringBuilder();
        Iterator<StreamEvent> events = model.stream(request);
        while (events.hasNext()) {
            StreamEvent event = events.next();
            if (event.getType() == StreamEvent.Type.CONTENT_BLOCK_DELTA && event.getData() != null) {
                Object delta = event.getData().get("delta");
                if (delta instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> deltaMap = (Map<String, Object>) delta;
                    Object text = deltaMap.get("text");
                    if (text != null) {
                        response.append(text);
                    }
                }
            }
        }

        return parseResponse(response.toString().trim());
    }

    private String buildPrompt(SteeringContext context, SteeringContextProvider.SteeringInput input) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current context state:\n");
        context.toMap().forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));

        if (input.toolName() != null) {
            sb.append("\nPending tool call: ").append(input.toolName());
            if (input.toolInput() != null) {
                sb.append("\nTool input: ").append(input.toolInput());
            }
        }

        if (input.messages() != null && !input.messages().isEmpty()) {
            Message last = input.messages().get(input.messages().size() - 1);
            sb.append("\nLast message: ").append(last.getTextContent());
        }

        sb.append("\n\nDecide: PROCEED, GUIDE <guidance>, or INTERRUPT <reason>");
        return sb.toString();
    }

    private SteeringAction parseResponse(String response) {
        String upper = response.toUpperCase();
        if (upper.startsWith("PROCEED")) {
            return SteeringAction.proceed();
        } else if (upper.startsWith("INTERRUPT")) {
            String reason = response.length() > 10 ? response.substring(10).trim() : "LLM decided to interrupt";
            return SteeringAction.interrupt(reason);
        } else if (upper.startsWith("GUIDE")) {
            String guidance = response.length() > 6 ? response.substring(6).trim() : response;
            return SteeringAction.guide(guidance);
        }
        return SteeringAction.guide(response);
    }

    private String buildDefaultPrompt() {
        return """
                You are a steering evaluator. Given the current agent context and pending action,
                decide how the agent should proceed. Respond with exactly one of:
                - PROCEED (allow the action)
                - GUIDE <guidance text> (allow but inject guidance)
                - INTERRUPT <reason> (stop execution)

                Base your decision on safety, relevance, and alignment with the task.""";
    }
}
