package com.strands.session;

import com.strands.types.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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

    public Snapshot(List<Message> messages, Map<String, Object> state,
                    Map<String, Object> conversationManagerState, String systemPrompt) {
        this.schemaVersion = SCHEMA_VERSION;
        this.messages = messages;
        this.state = state;
        this.conversationManagerState = conversationManagerState;
        this.systemPrompt = systemPrompt;
    }
}
