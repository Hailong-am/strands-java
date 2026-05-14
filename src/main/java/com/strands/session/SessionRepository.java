package com.strands.session;

import java.util.Optional;

public interface SessionRepository {

    Optional<Snapshot> load(String agentId, String sessionId);

    void save(String agentId, String sessionId, Snapshot snapshot);

    void delete(String agentId, String sessionId);
}
