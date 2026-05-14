package com.strands.session;

import com.strands.types.Message;

import java.util.List;
import java.util.Map;

public class Snapshot {

    private static final String SCHEMA_VERSION = "1.0";

    private String schemaVersion;
    private List<Message> messages;
    private Map<String, Object> state;
    private Map<String, Object> conversationManagerState;
    private String systemPrompt;

    public Snapshot() {
        this.schemaVersion = SCHEMA_VERSION;
    }

    public Snapshot(List<Message> messages, Map<String, Object> state,
                    Map<String, Object> conversationManagerState, String systemPrompt) {
        this.schemaVersion = SCHEMA_VERSION;
        this.messages = messages;
        this.state = state;
        this.conversationManagerState = conversationManagerState;
        this.systemPrompt = systemPrompt;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Map<String, Object> getState() {
        return state;
    }

    public void setState(Map<String, Object> state) {
        this.state = state;
    }

    public Map<String, Object> getConversationManagerState() {
        return conversationManagerState;
    }

    public void setConversationManagerState(Map<String, Object> conversationManagerState) {
        this.conversationManagerState = conversationManagerState;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}
