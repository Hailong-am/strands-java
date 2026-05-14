package com.strands.session;

import com.strands.types.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Snapshot {

    private static final String SCHEMA_VERSION = "1.0";

    private String schemaVersion = SCHEMA_VERSION;
    private List<Message> messages;
    private Map<String, Object> state;
    private Map<String, Object> conversationManagerState;
    private String systemPrompt;
    private Map<String, Object> interruptState;
    private Map<String, Object> appData;

    public Snapshot(List<Message> messages, Map<String, Object> state,
                    Map<String, Object> conversationManagerState, String systemPrompt) {
        this.schemaVersion = SCHEMA_VERSION;
        this.messages = messages;
        this.state = state;
        this.conversationManagerState = conversationManagerState;
        this.systemPrompt = systemPrompt;
    }

    public static Snapshot take(com.strands.agent.Agent agent) {
        return take(agent, null, null, null);
    }

    public static Snapshot take(com.strands.agent.Agent agent, String preset,
                                Set<String> include, Set<String> exclude) {
        Snapshot snapshot = new Snapshot();
        snapshot.schemaVersion = SCHEMA_VERSION;

        Set<String> fields = resolveFields(preset, include, exclude);

        if (fields.contains("messages")) {
            snapshot.messages = List.copyOf(agent.getMessages());
        }
        if (fields.contains("state")) {
            snapshot.state = Map.copyOf(agent.getState().toMap());
        }
        if (fields.contains("systemPrompt")) {
            snapshot.systemPrompt = agent.getSystemPrompt();
        }

        return snapshot;
    }

    public Snapshot withAppData(Map<String, Object> appData) {
        this.appData = appData;
        return this;
    }

    private static Set<String> resolveFields(String preset, Set<String> include, Set<String> exclude) {
        Set<String> fields;
        if ("session".equals(preset)) {
            fields = new java.util.HashSet<>(Set.of("messages", "state", "systemPrompt"));
        } else if (include != null && !include.isEmpty()) {
            fields = new java.util.HashSet<>(include);
        } else {
            fields = new java.util.HashSet<>(Set.of("messages", "state", "conversationManagerState", "systemPrompt"));
        }

        if (exclude != null) {
            fields.removeAll(exclude);
        }
        return fields;
    }
}
