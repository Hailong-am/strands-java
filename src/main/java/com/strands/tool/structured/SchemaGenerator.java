package com.strands.tool.structured;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class SchemaGenerator {

    public static Map<String, Object> generateSchema(Class<?> clazz) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;

            String name = field.getName();
            Map<String, Object> propSchema = fieldSchema(field.getType(), field.getGenericType());
            properties.put(name, propSchema);
            required.add(name);
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private static Map<String, Object> fieldSchema(Class<?> type, Type genericType) {
        Map<String, Object> schema = new LinkedHashMap<>();

        if (type == String.class) {
            schema.put("type", "string");
        } else if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
            schema.put("type", "integer");
        } else if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            schema.put("type", "number");
        } else if (type == boolean.class || type == Boolean.class) {
            schema.put("type", "boolean");
        } else if (List.class.isAssignableFrom(type)) {
            schema.put("type", "array");
            if (genericType instanceof ParameterizedType pt) {
                Type[] typeArgs = pt.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> itemClass) {
                    schema.put("items", fieldSchema(itemClass, itemClass));
                }
            }
        } else if (Map.class.isAssignableFrom(type)) {
            schema.put("type", "object");
        } else if (type.isEnum()) {
            schema.put("type", "string");
            List<String> enumValues = new ArrayList<>();
            for (Object constant : type.getEnumConstants()) {
                enumValues.add(constant.toString());
            }
            schema.put("enum", enumValues);
        } else {
            schema.put("type", "object");
            Map<String, Object> nested = generateSchema(type);
            schema.put("properties", nested.get("properties"));
            if (nested.containsKey("required")) {
                schema.put("required", nested.get("required"));
            }
        }

        return schema;
    }
}
