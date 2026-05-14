package com.strands.agent;

import com.strands.hook.HookRegistry;

public class NullConversationManager implements ConversationManager {

    @Override
    public void applyManagement(Agent agent) {
    }

    @Override
    public void reduceContext(Agent agent, Exception overflow) {
    }

    @Override
    public void registerHooks(HookRegistry registry) {
    }
}
