package com.strands.model;

import com.strands.types.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TokenEstimatorTest {

    @Test
    void testEstimateTokensFromNull() {
        assertEquals(0, TokenEstimator.estimateTokens((String) null));
    }

    @Test
    void testEstimateTokensFromEmpty() {
        assertEquals(0, TokenEstimator.estimateTokens(""));
    }

    @Test
    void testEstimateTokensFromText() {
        assertEquals(3, TokenEstimator.estimateTokens("Hello World!"));
    }

    @Test
    void testEstimateTokensRoundsUp() {
        assertEquals(2, TokenEstimator.estimateTokens("Hello"));
    }

    @Test
    void testEstimateTokensFromMessage() {
        Message msg = Message.user("Hello World! This is a test.");
        long tokens = TokenEstimator.estimateTokens(msg);
        assertTrue(tokens > 4); // overhead + text tokens
    }

    @Test
    void testEstimateTokensFromToolUseMessage() {
        ToolUse toolUse = new ToolUse();
        toolUse.setToolUseId("id-1");
        toolUse.setName("calculator");
        toolUse.setInput(Map.of("a", 1, "b", 2));

        Message msg = new Message(Message.Role.ASSISTANT, List.of(ContentBlock.fromToolUse(toolUse)));
        long tokens = TokenEstimator.estimateTokens(msg);
        assertTrue(tokens >= 54); // 4 overhead + 50 tool overhead
    }

    @Test
    void testEstimateTokensFromToolResultMessage() {
        ToolResult result = ToolResult.success("id-1", "The answer is 42");
        Message msg = Message.toolResult(result);
        long tokens = TokenEstimator.estimateTokens(msg);
        assertTrue(tokens > 4);
    }

    @Test
    void testEstimateTokensFromMessageList() {
        List<Message> messages = List.of(
                Message.user("Hello"),
                Message.assistant("Hi there"),
                Message.user("How are you?")
        );
        long tokens = TokenEstimator.estimateTokens(messages);
        assertTrue(tokens > 12); // 3 messages * 4 overhead minimum
    }

    @Test
    void testEstimateTokensForRequest() {
        String systemPrompt = "You are a helpful assistant.";
        List<Message> messages = List.of(Message.user("Hello"));
        long tokens = TokenEstimator.estimateTokensForRequest(systemPrompt, messages);
        assertTrue(tokens > 10); // system + message + overhead
    }
}
