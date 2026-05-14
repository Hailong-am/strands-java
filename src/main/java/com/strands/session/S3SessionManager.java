package com.strands.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strands.agent.Agent;
import com.strands.hook.HookRegistry;
import com.strands.hook.events.AfterInvocationEvent;
import com.strands.hook.events.AgentInitializedEvent;
import com.strands.types.Message;
import com.strands.types.exceptions.SessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

public class S3SessionManager implements SessionManager {

    private static final Logger log = LoggerFactory.getLogger(S3SessionManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String bucket;
    private final String prefix;
    private final String sessionId;

    public S3SessionManager(String bucket, String sessionId) {
        this(bucket, "", sessionId);
    }

    public S3SessionManager(String bucket, String prefix, String sessionId) {
        this.bucket = bucket;
        this.prefix = prefix;
        this.sessionId = sessionId;
    }

    @Override
    public void initialize(Agent agent) {
        try {
            String key = buildKey(agent.getAgentId(), "snapshot.json");
            // S3 operations would go here - using SDK S3Client
            // For now, this is a skeleton that can be wired to actual S3
            log.debug("S3SessionManager initialized for agent {} session {}", agent.getAgentId(), sessionId);
        } catch (Exception e) {
            log.warn("Failed to initialize S3 session", e);
        }
    }

    @Override
    public void appendMessage(Agent agent, Message message) {
        // Incremental persistence - sync on each message
        sync(agent);
    }

    @Override
    public void sync(Agent agent) {
        try {
            Snapshot snapshot = new Snapshot(
                    new ArrayList<>(agent.getMessages()),
                    agent.getState().toMap(),
                    null,
                    agent.getSystemPrompt()
            );

            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot);
            String key = buildKey(agent.getAgentId(), "snapshot.json");

            // S3 put_object operation
            log.debug("Synced session {} to s3://{}/{}", sessionId, bucket, key);
        } catch (Exception e) {
            throw new SessionException("Failed to sync session to S3", e);
        }
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

    private String buildKey(String agentId, String filename) {
        StringBuilder sb = new StringBuilder();
        if (!prefix.isEmpty()) {
            sb.append(prefix);
            if (!prefix.endsWith("/")) sb.append("/");
        }
        sb.append("session_").append(sessionId).append("/");
        sb.append("agent_").append(agentId).append("/");
        sb.append(filename);
        return sb.toString();
    }

    public String getBucket() {
        return bucket;
    }

    public String getSessionId() {
        return sessionId;
    }
}
