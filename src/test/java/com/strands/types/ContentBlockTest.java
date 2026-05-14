package com.strands.types;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContentBlockTest {

    @Test
    void testFromText() {
        ContentBlock block = ContentBlock.fromText("hello");
        assertTrue(block.isText());
        assertFalse(block.isToolUse());
        assertFalse(block.isToolResult());
        assertEquals("hello", block.getText());
    }

    @Test
    void testFromToolUse() {
        ToolUse toolUse = new ToolUse();
        toolUse.setToolUseId("id-1");
        toolUse.setName("search");
        toolUse.setInput(Map.of("query", "test"));

        ContentBlock block = ContentBlock.fromToolUse(toolUse);
        assertTrue(block.isToolUse());
        assertFalse(block.isText());
        assertEquals("search", block.getToolUse().getName());
    }

    @Test
    void testFromToolResult() {
        ToolResult result = ToolResult.success("id-1", "found it");
        ContentBlock block = ContentBlock.fromToolResult(result);

        assertTrue(block.isToolResult());
        assertFalse(block.isText());
        assertEquals(ToolResult.Status.SUCCESS, block.getToolResult().getStatus());
    }

    @Test
    void testFromImage() {
        ImageContent image = new ImageContent("png", new byte[]{1, 2, 3});
        ContentBlock block = ContentBlock.fromImage(image);

        assertNotNull(block.getImage());
        assertNull(block.getText());
    }

    @Test
    void testFromDocument() {
        DocumentContent doc = new DocumentContent("pdf", "doc.pdf", new byte[]{1});
        ContentBlock block = ContentBlock.fromDocument(doc);

        assertNotNull(block.getDocument());
    }

    @Test
    void testFromVideo() {
        VideoContent video = new VideoContent("mp4", new byte[]{1, 2}, null);
        ContentBlock block = ContentBlock.fromVideo(video);

        assertNotNull(block.getVideo());
    }

    @Test
    void testFromReasoning() {
        ReasoningContent reasoning = new ReasoningContent("thinking hard...");
        ContentBlock block = ContentBlock.fromReasoning(reasoning);

        assertNotNull(block.getReasoningContent());
    }
}
