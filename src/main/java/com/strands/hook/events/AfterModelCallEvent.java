package com.strands.hook.events;

import com.strands.hook.HookEvent;
import com.strands.types.Message;
import com.strands.types.StopReason;

public class AfterModelCallEvent extends HookEvent {

    private final Message message;
    private final StopReason stopReason;
    private boolean retry;

    public AfterModelCallEvent(Message message, StopReason stopReason) {
        this.message = message;
        this.stopReason = stopReason;
    }

    public Message getMessage() {
        return message;
    }

    public StopReason getStopReason() {
        return stopReason;
    }

    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }
}
