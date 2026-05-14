package com.strands.hook;

public abstract class HookEvent {

    private boolean consumed;

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }
}
