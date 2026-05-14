package com.strands.session;

import com.strands.agent.Agent;
import com.strands.hook.HookProvider;
import com.strands.types.Message;

public interface SessionManager extends HookProvider {

    void initialize(Agent agent);

    void appendMessage(Agent agent, Message message);

    void sync(Agent agent);
}
