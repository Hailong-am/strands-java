package com.strands.experimental.checkpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * File-system-based checkpoint store. Stores each checkpoint as a JSON file.
 */
public class FileCheckpointStore implements CheckpointStore {

    private static final Logger log = LoggerFactory.getLogger(FileCheckpointStore.class);

    private final Path directory;
    private final ObjectMapper mapper;

    public FileCheckpointStore(Path directory) {
        this.directory = directory;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create checkpoint directory: " + directory, e);
        }
    }

    @Override
    public void save(Checkpoint checkpoint) {
        Path file = directory.resolve(checkpoint.getCheckpointId() + ".json");
        try {
            mapper.writeValue(file.toFile(), checkpoint);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save checkpoint: " + checkpoint.getCheckpointId(), e);
        }
    }

    @Override
    public Optional<Checkpoint> load(String checkpointId) {
        Path file = directory.resolve(checkpointId + ".json");
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.of(mapper.readValue(file.toFile(), Checkpoint.class));
        } catch (IOException e) {
            log.error("Failed to load checkpoint: {}", checkpointId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Checkpoint> loadLatest(String agentId, String sessionId) {
        return listBySession(sessionId).stream()
                .filter(c -> agentId.equals(c.getAgentId()))
                .max(Comparator.comparing(Checkpoint::getTimestamp));
    }

    @Override
    public List<Checkpoint> listBySession(String sessionId) {
        List<Checkpoint> results = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(f -> f.toString().endsWith(".json"))
                    .forEach(f -> {
                        try {
                            Checkpoint cp = mapper.readValue(f.toFile(), Checkpoint.class);
                            if (sessionId.equals(cp.getSessionId())) {
                                results.add(cp);
                            }
                        } catch (IOException e) {
                            log.warn("Failed to read checkpoint file: {}", f, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to list checkpoints for session: {}", sessionId, e);
        }
        return results;
    }

    @Override
    public void delete(String checkpointId) {
        try {
            Files.deleteIfExists(directory.resolve(checkpointId + ".json"));
        } catch (IOException e) {
            log.warn("Failed to delete checkpoint: {}", checkpointId, e);
        }
    }

    @Override
    public void deleteBySession(String sessionId) {
        listBySession(sessionId).forEach(cp -> delete(cp.getCheckpointId()));
    }
}
