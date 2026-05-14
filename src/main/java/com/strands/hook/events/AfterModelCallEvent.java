package com.strands.hook.events;

import com.strands.hook.HookEvent;
import com.strands.types.Message;
import com.strands.types.StopReason;

public class AfterModelCallEvent extends HookEvent {

    private final Message message;
    private final StopReason stopReason;
    private final Exception exception;
    private boolean retry;

    public AfterModelCallEvent(Message message, StopReason stopReason) {
        this(message, stopReason, null);
    }

    public AfterModelCallEvent(Message message, StopReason stopReason, Exception exception) {
        this.message = message;
        this.stopReason = stopReason;
        this.exception = exception;
    }

    public Message getMessage() {
        return message;
    }

    public StopReason getStopReason() {
        return stopReason;
    }

    public Exception getException() {
        return exception;
    }

    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }
}
