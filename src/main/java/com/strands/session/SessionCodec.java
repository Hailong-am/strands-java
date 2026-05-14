package com.strands.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Encodes and decodes session data including binary content (images, audio, documents).
 * Uses Base64 encoding for binary data in JSON-safe format.
 */
public class SessionCodec {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String encode(Snapshot snapshot) throws JsonProcessingException {
        return mapper.writeValueAsString(snapshot);
    }

    public static Snapshot decode(String json) throws IOException {
        return mapper.readValue(json, Snapshot.class);
    }

    public static byte[] encodeToBytes(Snapshot snapshot) throws JsonProcessingException {
        return mapper.writeValueAsBytes(snapshot);
    }

    public static Snapshot decodeFromBytes(byte[] bytes) throws IOException {
        return mapper.readValue(bytes, Snapshot.class);
    }

    public static String encodeBytes(byte[] data) {
        if (data == null) return null;
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] decodeBytes(String encoded) {
        if (encoded == null) return null;
        return Base64.getDecoder().decode(encoded);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> snapshotToMap(Snapshot snapshot) throws JsonProcessingException {
        String json = mapper.writeValueAsString(snapshot);
        return mapper.readValue(json, Map.class);
    }

    public static Snapshot mapToSnapshot(Map<String, Object> map) throws JsonProcessingException {
        String json = mapper.writeValueAsString(map);
        try {
            return mapper.readValue(json, Snapshot.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize snapshot from map", e);
        }
    }
}
