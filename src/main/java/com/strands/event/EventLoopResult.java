package com.strands.event;

import com.strands.types.Message;
import com.strands.types.Metrics;
import com.strands.types.StopReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class EventLoopResult {

    private final Message message;
    private final StopReason stopReason;
    private final Metrics metrics;
    private final Map<String, Object> invocationProperties;
}
