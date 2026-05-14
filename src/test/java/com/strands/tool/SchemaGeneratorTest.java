package com.strands.tool;

import com.strands.tool.structured.SchemaGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class SchemaGeneratorTest {

    @Test
    void testSimpleClass() {
        Map<String, Object> schema = SchemaGenerator.generateSchema(SimpleDTO.class);

        assertEquals("object", schema.get("type"));
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        assertNotNull(props);
        assertTrue(props.containsKey("name"));
        assertTrue(props.containsKey("age"));

        Map<String, Object> nameProp = (Map<String, Object>) props.get("name");
        assertEquals("string", nameProp.get("type"));

        Map<String, Object> ageProp = (Map<String, Object>) props.get("age");
        assertEquals("integer", ageProp.get("type"));
    }

    @Test
    void testBooleanField() {
        Map<String, Object> schema = SchemaGenerator.generateSchema(WithBoolean.class);
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");

        Map<String, Object> activeProp = (Map<String, Object>) props.get("active");
        assertEquals("boolean", activeProp.get("type"));
    }

    @Test
    void testDoubleField() {
        Map<String, Object> schema = SchemaGenerator.generateSchema(WithDouble.class);
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");

        Map<String, Object> scoreProp = (Map<String, Object>) props.get("score");
        assertEquals("number", scoreProp.get("type"));
    }

    @Test
    void testListField() {
        Map<String, Object> schema = SchemaGenerator.generateSchema(WithList.class);
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");

        Map<String, Object> tagsProp = (Map<String, Object>) props.get("tags");
        assertEquals("array", tagsProp.get("type"));
        Map<String, Object> items = (Map<String, Object>) tagsProp.get("items");
        assertEquals("string", items.get("type"));
    }

    @Test
    void testEnumField() {
        Map<String, Object> schema = SchemaGenerator.generateSchema(WithEnum.class);
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");

        Map<String, Object> statusProp = (Map<String, Object>) props.get("status");
        assertEquals("string", statusProp.get("type"));
        List<String> enumValues = (List<String>) statusProp.get("enum");
        assertTrue(enumValues.contains("ACTIVE"));
        assertTrue(enumValues.contains("INACTIVE"));
    }

    @Test
    void testNestedObject() {
        Map<String, Object> schema = SchemaGenerator.generateSchema(WithNested.class);
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");

        Map<String, Object> addressProp = (Map<String, Object>) props.get("address");
        assertEquals("object", addressProp.get("type"));
        Map<String, Object> nestedProps = (Map<String, Object>) addressProp.get("properties");
        assertTrue(nestedProps.containsKey("city"));
        assertTrue(nestedProps.containsKey("zip"));
    }

    @Test
    void testRequiredFields() {
        Map<String, Object> schema = SchemaGenerator.generateSchema(SimpleDTO.class);
        List<String> required = (List<String>) schema.get("required");
        assertNotNull(required);
        assertTrue(required.contains("name"));
        assertTrue(required.contains("age"));
    }

    @Test
    void testMapField() {
        Map<String, Object> schema = SchemaGenerator.generateSchema(WithMap.class);
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");

        Map<String, Object> dataProp = (Map<String, Object>) props.get("data");
        assertEquals("object", dataProp.get("type"));
    }

    static class SimpleDTO {
        String name;
        int age;
    }

    static class WithBoolean {
        boolean active;
    }

    static class WithDouble {
        double score;
    }

    static class WithList {
        List<String> tags;
    }

    enum Status { ACTIVE, INACTIVE }

    static class WithEnum {
        Status status;
    }

    static class Address {
        String city;
        String zip;
    }

    static class WithNested {
        Address address;
    }

    static class WithMap {
        Map<String, Object> data;
    }
}
