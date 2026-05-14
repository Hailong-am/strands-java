package com.strands.model;

import com.strands.types.Message;
import com.strands.types.StopReason;
import com.strands.types.Usage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StreamResult {

    private final Message message;
    private final StopReason stopReason;
    private final Usage usage;
    private final long latencyMs;
}
