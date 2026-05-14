package com.strands.types;

import java.util.regex.Pattern;

/**
 * Validates IDs used in the agent system. Ensures tool use IDs and other
 * identifiers conform to expected formats to prevent API errors.
 */
public final class IdValidator {

    private static final Pattern TOOL_USE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private IdValidator() {}

    public static boolean isValidToolUseId(String id) {
        if (id == null || id.isEmpty()) return false;
        return TOOL_USE_ID_PATTERN.matcher(id).matches();
    }

    public static boolean isValidUUID(String id) {
        if (id == null || id.isEmpty()) return false;
        return UUID_PATTERN.matcher(id).matches();
    }

    public static String sanitizeToolUseId(String id) {
        if (id == null) return generateToolUseId();
        String sanitized = id.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (sanitized.length() > 64) {
            sanitized = sanitized.substring(0, 64);
        }
        if (sanitized.isEmpty()) {
            return generateToolUseId();
        }
        return sanitized;
    }

    public static String generateToolUseId() {
        return "toolu_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
