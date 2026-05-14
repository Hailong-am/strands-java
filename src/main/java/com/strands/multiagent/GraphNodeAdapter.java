package com.strands.multiagent;

import com.strands.agent.Agent;
import com.strands.agent.AgentResult;
import com.strands.model.Model;
import com.strands.model.ModelConfig;
import com.strands.model.StreamRequest;
import com.strands.types.ContentBlock;
import com.strands.types.Message;
import com.strands.types.streaming.StreamEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wraps a GraphAgent (or any MultiAgent) as a single Agent node so it can be
 * used inside another GraphAgent, enabling nested graph compositions.
 */
public class GraphNodeAdapter {

    public static Agent wrapAsAgent(MultiAgent multiAgent, String name) {
        Model delegatingModel = new Model() {
            private String lastPrompt;

            @Override
            public Iterator<StreamEvent> stream(StreamRequest request) {
                List<Message> messages = request.getMessages();
                String prompt = "";
                if (!messages.isEmpty()) {
                    Message last = messages.get(messages.size() - 1);
                    prompt = last.getTextContent();
                }

                AgentResult result = multiAgent.invoke(prompt);
                String responseText = result.toString();

                return List.of(
                        StreamEvent.messageStart("assistant"),
                        StreamEvent.contentBlockStart(0, Map.of()),
                        StreamEvent.contentBlockDelta(0, Map.of("text", responseText)),
                        StreamEvent.contentBlockStop(0),
                        StreamEvent.messageStop("end_turn"),
                        StreamEvent.metadata(0, 0, 0)
                ).iterator();
            }

            @Override
            public ModelConfig getConfig() {
                return new ModelConfig("nested-graph");
            }

            @Override
            public void updateConfig(Map<String, Object> configUpdates) {
            }
        };

        return Agent.builder()
                .name(name)
                .model(delegatingModel)
                .build();
    }
}
