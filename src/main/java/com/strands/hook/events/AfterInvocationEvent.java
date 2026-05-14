package com.strands.hook.events;

import com.strands.hook.HookEvent;

public class AfterInvocationEvent extends HookEvent {

    private final Object agent;
    private boolean resume;
    private String resumeInput;

    public AfterInvocationEvent(Object agent) {
        this.agent = agent;
    }

    public Object getAgent() {
        return agent;
    }

    public boolean isResume() {
        return resume;
    }

    public void setResume(boolean resume) {
        this.resume = resume;
    }

    public String getResumeInput() {
        return resumeInput;
    }

    public void setResumeInput(String resumeInput) {
        this.resumeInput = resumeInput;
    }
}
