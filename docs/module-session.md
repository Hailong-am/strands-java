# Module: Session Management

## Overview

Session management persists agent state (messages, AgentState, conversation manager state)
across invocations. It integrates via the hook system.

## Key Interfaces

### SessionManager
```java
public interface SessionManager extends HookProvider {
    void initialize(Agent agent);
    void appendMessage(Agent agent, Message message);
    void sync(Agent agent);
}
```

### Snapshot
Serializable capture of agent state:
```java
public class Snapshot {
    String schemaVersion;  // "1.0"
    List<Message> messages;
    Map<String, Object> state;
    Map<String, Object> conversationManagerState;
    String systemPrompt;
}
```

## Implementations

### FileSessionManager
- Stores snapshots as JSON files in a local directory
- Path: `{baseDir}/{agentId}/{sessionId}.json`
- Hooks into `AgentInitializedEvent` to restore, `MessageAddedEvent` to persist

### SessionRepository (SPI)
```java
public interface SessionRepository {
    Optional<Snapshot> load(String agentId, String sessionId);
    void save(String agentId, String sessionId, Snapshot snapshot);
    void delete(String agentId, String sessionId);
}
```

Custom backends (S3, DynamoDB, etc.) implement this interface and wrap with
`RepositorySessionManager`.

## Hook Integration

| Hook Event | Action |
|-----------|--------|
| `AgentInitializedEvent` | Load snapshot, restore agent state |
| `MessageAddedEvent` | Persist incrementally |
| `AfterInvocationEvent` | Full sync |
