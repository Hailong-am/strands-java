package com.strands.tool;

import com.strands.types.ToolSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToolValidator {

    private static final Logger log = LoggerFactory.getLogger(ToolValidator.class);

    public static List<String> validate(ToolSpec spec) {
        List<String> errors = new ArrayList<>();

        if (spec.getName() == null || spec.getName().isBlank()) {
            errors.add("Tool name is required");
        } else if (!spec.getName().matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            errors.add("Tool name must match pattern ^[a-zA-Z_][a-zA-Z0-9_]*$: " + spec.getName());
        }

        if (spec.getDescription() == null || spec.getDescription().isBlank()) {
            errors.add("Tool description is required");
        }

        if (spec.getInputSchema() != null) {
            validateSchema(spec.getInputSchema(), "inputSchema", errors);
        }

        return errors;
    }

    @SuppressWarnings("unchecked")
    private static void validateSchema(Map<String, Object> schema, String path, List<String> errors) {
        Object type = schema.get("type");
        if (type == null) {
            errors.add(path + ": 'type' is required");
            return;
        }

        if ("object".equals(type)) {
            Object properties = schema.get("properties");
            if (properties != null && !(properties instanceof Map)) {
                errors.add(path + ".properties: must be an object");
            }
        }
    }

    public static boolean isValid(ToolSpec spec) {
        return validate(spec).isEmpty();
    }
}
