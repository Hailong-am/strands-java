package com.strands.agent;

import com.strands.hook.HookRegistry;
import com.strands.model.Model;
import com.strands.model.StreamProcessor;
import com.strands.model.StreamRequest;
import com.strands.model.StreamResult;
import com.strands.types.ContentBlock;
import com.strands.types.Message;
import com.strands.types.streaming.StreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SummarizingConversationManager implements ConversationManager {

    private static final Logger log = LoggerFactory.getLogger(SummarizingConversationManager.class);
    private static final String DEFAULT_SUMMARIZATION_PROMPT = "Please summarize this conversation concisely, "
            + "preserving key information, decisions made, and any important context needed to continue the conversation.";

    private final double summaryRatio;
    private final int preserveRecentMessages;
    private final String summarizationSystemPrompt;
    private Message summaryMessage;

    public SummarizingConversationManager() {
        this(0.3, 10, null);
    }

    public SummarizingConversationManager(double summaryRatio, int preserveRecentMessages, String summarizationSystemPrompt) {
        this.summaryRatio = summaryRatio;
        this.preserveRecentMessages = preserveRecentMessages;
        this.summarizationSystemPrompt = summarizationSystemPrompt != null
                ? summarizationSystemPrompt
                : "You are a conversation summarizer. Provide concise summaries preserving key context.";
    }

    @Override
    public void applyManagement(Agent agent) {
        // No proactive management — only reactive via reduceContext
    }

    @Override
    public void reduceContext(Agent agent, Exception overflow) {
        List<Message> messages = agent.getMessages();
        if (messages.size() <= preserveRecentMessages) {
            if (overflow != null) throw new RuntimeException("Cannot reduce context further", overflow);
            return;
        }

        int splitPoint = Math.max(1, (int) (messages.size() * summaryRatio));
        splitPoint = adjustSplitPointForToolPairs(messages, splitPoint);

        List<Message> toSummarize = new ArrayList<>(messages.subList(0, splitPoint));
        List<Message> toKeep = new ArrayList<>(messages.subList(splitPoint, messages.size()));

        try {
            Message summary = summarize(agent.getModel(), toSummarize);
            this.summaryMessage = summary;

            messages.clear();
            messages.add(summary);
            messages.addAll(toKeep);

            log.info("Reduced context from {} to {} messages via summarization", toSummarize.size() + toKeep.size(), messages.size());
        } catch (Exception e) {
            if (overflow != null) {
                throw new RuntimeException("Summarization failed during context reduction", e);
            }
            log.warn("Proactive summarization failed, skipping", e);
        }
    }

    @Override
    public void registerHooks(HookRegistry registry) {
    }

    private Message summarize(Model model, List<Message> messagesToSummarize) {
        List<Message> summarizationMessages = new ArrayList<>(messagesToSummarize);
        summarizationMessages.add(Message.user(DEFAULT_SUMMARIZATION_PROMPT));

        StreamRequest request = StreamRequest.builder()
                .messages(summarizationMessages)
                .systemPrompt(summarizationSystemPrompt)
                .build();

        Iterator<StreamEvent> stream = model.stream(request);
        StreamProcessor processor = new StreamProcessor();
        StreamResult result = processor.process(stream, null);

        String summaryText = result.getMessage().getTextContent();
        return Message.user("[Conversation Summary]\n" + summaryText);
    }

    private int adjustSplitPointForToolPairs(List<Message> messages, int splitPoint) {
        while (splitPoint < messages.size() - preserveRecentMessages) {
            Message msg = messages.get(splitPoint);
            if (hasToolResult(msg) && splitPoint > 0 && hasToolUse(messages.get(splitPoint - 1))) {
                splitPoint++;
                continue;
            }
            if (hasToolUse(msg) && splitPoint + 1 < messages.size() && hasToolResult(messages.get(splitPoint + 1))) {
                splitPoint += 2;
                continue;
            }
            break;
        }
        return Math.min(splitPoint, messages.size() - preserveRecentMessages);
    }

    private boolean hasToolUse(Message msg) {
        return msg.getContent().stream().anyMatch(ContentBlock::isToolUse);
    }

    private boolean hasToolResult(Message msg) {
        return msg.getContent().stream().anyMatch(ContentBlock::isToolResult);
    }

    public Message getSummaryMessage() {
        return summaryMessage;
    }
}
