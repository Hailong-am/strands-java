package com.strands.model;

import com.strands.types.ContentBlock;
import com.strands.types.Message;

import java.util.List;

public class TokenEstimator {

    private static final double CHARS_PER_TOKEN = 4.0;
    private static final int OVERHEAD_PER_MESSAGE = 4;

    public static long estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (long) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }

    public static long estimateTokens(Message message) {
        long tokens = OVERHEAD_PER_MESSAGE;
        for (ContentBlock block : message.getContent()) {
            if (block.isText()) {
                tokens += estimateTokens(block.getText());
            } else if (block.isToolUse()) {
                tokens += estimateTokens(block.getToolUse().getName());
                tokens += 50; // tool use overhead
            } else if (block.isToolResult()) {
                if (block.getToolResult().getContent() != null) {
                    for (var c : block.getToolResult().getContent()) {
                        if (c.getText() != null) {
                            tokens += estimateTokens(c.getText());
                        }
                    }
                }
            }
        }
        return tokens;
    }

    public static long estimateTokens(List<Message> messages) {
        long total = 0;
        for (Message msg : messages) {
            total += estimateTokens(msg);
        }
        return total;
    }

    public static long estimateTokensForRequest(String systemPrompt, List<Message> messages) {
        long tokens = estimateTokens(systemPrompt);
        tokens += estimateTokens(messages);
        tokens += 10; // request framing overhead
        return tokens;
    }
}
