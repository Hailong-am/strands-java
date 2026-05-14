package com.strands.agent;

import com.strands.types.Message;
import com.strands.types.Metrics;
import com.strands.types.StopReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AgentResult {

    private final StopReason stopReason;
    private final Message message;
    private final Metrics metrics;
    private final AgentState state;

    @Override
    public String toString() {
        if (message == null) return "";
        return message.getTextContent();
    }
}
