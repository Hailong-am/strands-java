package com.strands.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strands.agent.Agent;
import com.strands.hook.HookRegistry;
import com.strands.hook.events.AfterInvocationEvent;
import com.strands.hook.events.AgentInitializedEvent;
import com.strands.hook.events.MessageAddedEvent;
import com.strands.types.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class FileSessionManager implements SessionManager {

    private static final Logger log = LoggerFactory.getLogger(FileSessionManager.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path baseDir;
    private final String sessionId;

    public FileSessionManager(Path baseDir, String sessionId) {
        this.baseDir = baseDir;
        this.sessionId = sessionId;
    }

    @Override
    public void initialize(Agent agent) {
        Path snapshotPath = getSnapshotPath(agent.getAgentId());
        if (Files.exists(snapshotPath)) {
            try {
                Snapshot snapshot = objectMapper.readValue(snapshotPath.toFile(), Snapshot.class);
                if (snapshot.getMessages() != null) {
                    agent.getMessages().clear();
                    agent.getMessages().addAll(snapshot.getMessages());
                }
                if (snapshot.getState() != null) {
                    agent.getState().loadFrom(snapshot.getState());
                }
                log.debug("Restored session {} for agent {}", sessionId, agent.getAgentId());
            } catch (IOException e) {
                log.warn("Failed to load session snapshot", e);
            }
        }
    }

    @Override
    public void appendMessage(Agent agent, Message message) {
        sync(agent);
    }

    @Override
    public void sync(Agent agent) {
        Snapshot snapshot = new Snapshot(
                new ArrayList<>(agent.getMessages()),
                agent.getState().toMap(),
                null,
                agent.getSystemPrompt()
        );

        Path snapshotPath = getSnapshotPath(agent.getAgentId());
        try {
            Files.createDirectories(snapshotPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(snapshotPath.toFile(), snapshot);
        } catch (IOException e) {
            log.error("Failed to save session snapshot", e);
        }
    }

    @Override
    public void registerHooks(HookRegistry registry) {
        registry.register(AgentInitializedEvent.class, event -> {
            if (event.getAgent() instanceof Agent agent) {
                initialize(agent);
            }
        });
        registry.register(MessageAddedEvent.class, event -> {
            // Note: we'd need agent reference here; handled via sync
        });
        registry.register(AfterInvocationEvent.class, event -> {
            if (event.getAgent() instanceof Agent agent) {
                sync(agent);
            }
        });
    }

    private Path getSnapshotPath(String agentId) {
        return baseDir.resolve(agentId).resolve(sessionId + ".json");
    }
}
