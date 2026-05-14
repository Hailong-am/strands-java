package com.strands.session;

import com.strands.agent.Agent;
import com.strands.hook.HookRegistry;
import com.strands.hook.events.AfterInvocationEvent;
import com.strands.hook.events.AgentInitializedEvent;
import com.strands.types.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;

public class RepositorySessionManager implements SessionManager {

    private static final Logger log = LoggerFactory.getLogger(RepositorySessionManager.class);

    private final SessionRepository repository;
    private final String sessionId;

    public RepositorySessionManager(SessionRepository repository, String sessionId) {
        this.repository = repository;
        this.sessionId = sessionId;
    }

    @Override
    public void initialize(Agent agent) {
        Optional<Snapshot> snapshot = repository.load(agent.getAgentId(), sessionId);
        snapshot.ifPresent(s -> {
            if (s.getMessages() != null) {
                agent.getMessages().clear();
                agent.getMessages().addAll(s.getMessages());
            }
            if (s.getState() != null) {
                agent.getState().loadFrom(s.getState());
            }
            log.debug("Restored session {} for agent {}", sessionId, agent.getAgentId());
        });
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
        repository.save(agent.getAgentId(), sessionId, snapshot);
    }

    @Override
    public void registerHooks(HookRegistry registry) {
        registry.register(AgentInitializedEvent.class, event -> {
            if (event.getAgent() instanceof Agent agent) {
                initialize(agent);
            }
        });
        registry.register(AfterInvocationEvent.class, event -> {
            if (event.getAgent() instanceof Agent agent) {
                sync(agent);
            }
        });
    }
}
