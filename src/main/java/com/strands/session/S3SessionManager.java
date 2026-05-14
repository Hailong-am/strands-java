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
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class S3SessionManager implements SessionManager {

    private static final Logger log = LoggerFactory.getLogger(S3SessionManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final S3Client s3Client;
    private final String bucket;
    private final String prefix;
    private final String sessionId;

    public S3SessionManager(S3Client s3Client, String bucket, String sessionId) {
        this(s3Client, bucket, "", sessionId);
    }

    public S3SessionManager(S3Client s3Client, String bucket, String prefix, String sessionId) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.prefix = prefix;
        this.sessionId = sessionId;
    }

    @Override
    public void initialize(Agent agent) {
        try {
            String key = buildKey(agent.getAgentId(), "snapshot.json");
            if (objectExists(key)) {
                String json = getObject(key);
                Snapshot snapshot = MAPPER.readValue(json, Snapshot.class);
                if (snapshot.getMessages() != null) {
                    agent.getMessages().clear();
                    agent.getMessages().addAll(snapshot.getMessages());
                }
                if (snapshot.getState() != null) {
                    agent.getState().loadFrom(snapshot.getState());
                }
                log.debug("Restored session {} for agent {} from S3", sessionId, agent.getAgentId());
            } else {
                log.debug("No existing session {} found for agent {}, starting fresh", sessionId, agent.getAgentId());
            }
        } catch (Exception e) {
            log.warn("Failed to initialize S3 session, starting fresh", e);
        }
    }

    @Override
    public void appendMessage(Agent agent, Message message) {
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

            putObject(key, json);
            log.debug("Synced session {} to s3://{}/{}", sessionId, bucket, key);
        } catch (Exception e) {
            throw new SessionException("Failed to sync session to S3", e);
        }
    }

    public void delete(String agentId) {
        try {
            String keyPrefix = buildKey(agentId, "");
            ListObjectsV2Response listing = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(keyPrefix)
                    .build());

            List<ObjectIdentifier> toDelete = listing.contents().stream()
                    .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                    .toList();

            if (!toDelete.isEmpty()) {
                s3Client.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(bucket)
                        .delete(Delete.builder().objects(toDelete).build())
                        .build());
                log.debug("Deleted session {} for agent {} ({} objects)", sessionId, agentId, toDelete.size());
            }
        } catch (Exception e) {
            throw new SessionException("Failed to delete session from S3", e);
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

    private void putObject(String key, String content) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("application/json")
                        .build(),
                RequestBody.fromString(content, StandardCharsets.UTF_8)
        );
    }

    private String getObject(String key) {
        return s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        ).asUtf8String();
    }

    private boolean objectExists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
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
