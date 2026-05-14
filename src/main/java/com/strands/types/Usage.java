package com.strands.types;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class Usage {

    private long inputTokens;
    private long outputTokens;
    private long totalTokens;

    public Usage(long inputTokens, long outputTokens) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = inputTokens + outputTokens;
    }

    public void setInputTokens(long inputTokens) {
        this.inputTokens = inputTokens;
        this.totalTokens = this.inputTokens + this.outputTokens;
    }

    public void setOutputTokens(long outputTokens) {
        this.outputTokens = outputTokens;
        this.totalTokens = this.inputTokens + this.outputTokens;
    }

    public void accumulate(Usage other) {
        this.inputTokens += other.inputTokens;
        this.outputTokens += other.outputTokens;
        this.totalTokens = this.inputTokens + this.outputTokens;
    }
}
