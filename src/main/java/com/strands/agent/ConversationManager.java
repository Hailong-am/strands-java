package com.strands.agent;

import com.strands.hook.HookProvider;

public interface ConversationManager extends HookProvider {

    void applyManagement(Agent agent);

    void reduceContext(Agent agent, Exception overflow);
}
