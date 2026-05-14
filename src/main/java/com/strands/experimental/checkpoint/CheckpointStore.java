package com.strands.experimental.checkpoint;

import java.util.List;
import java.util.Optional;

/**
 * Storage backend for checkpoints. Implementations might use a database,
 * file system, or object store.
 */
public interface CheckpointStore {

    void save(Checkpoint checkpoint);

    Optional<Checkpoint> load(String checkpointId);

    Optional<Checkpoint> loadLatest(String agentId, String sessionId);

    List<Checkpoint> listBySession(String sessionId);

    void delete(String checkpointId);

    void deleteBySession(String sessionId);
}
