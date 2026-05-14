package com.strands.agent;

import com.strands.hook.HookRegistry;
import com.strands.types.ContentBlock;
import com.strands.types.Message;

import java.util.List;

public class SlidingWindowConversationManager implements ConversationManager {

    private final int windowSize;

    public SlidingWindowConversationManager() {
        this(40);
    }

    public SlidingWindowConversationManager(int windowSize) {
        this.windowSize = windowSize;
    }

    @Override
    public void applyManagement(Agent agent) {
        List<Message> messages = agent.getMessages();
        if (messages.size() <= windowSize) {
            return;
        }

        int removeCount = messages.size() - windowSize;
        int i = 0;
        while (i < removeCount && i < messages.size()) {
            Message msg = messages.get(i);
            if (hasToolUseOrResult(msg)) {
                int pairEnd = findToolPairEnd(messages, i);
                if (pairEnd >= removeCount) {
                    break;
                }
                i = pairEnd + 1;
            } else {
                i++;
            }
        }

        if (i > 0) {
            messages.subList(0, i).clear();
        }
    }

    @Override
    public void reduceContext(Agent agent, Exception overflow) {
        List<Message> messages = agent.getMessages();
        int reduceBy = Math.max(messages.size() / 4, 1);
        int i = 0;
        int removed = 0;
        while (removed < reduceBy && i < messages.size()) {
            Message msg = messages.get(i);
            if (hasToolUseOrResult(msg)) {
                int pairEnd = findToolPairEnd(messages, i);
                messages.subList(i, pairEnd + 1).clear();
                removed += (pairEnd - i + 1);
            } else {
                messages.remove(i);
                removed++;
            }
        }
    }

    @Override
    public void registerHooks(HookRegistry registry) {
    }

    private boolean hasToolUseOrResult(Message msg) {
        for (ContentBlock block : msg.getContent()) {
            if (block.isToolUse() || block.isToolResult()) {
                return true;
            }
        }
        return false;
    }

    private int findToolPairEnd(List<Message> messages, int start) {
        for (int i = start + 1; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (!hasToolUseOrResult(msg)) {
                return i - 1;
            }
        }
        return messages.size() - 1;
    }
}
