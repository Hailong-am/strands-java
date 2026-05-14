package com.strands.multiagent;

import com.strands.agent.AgentResult;

public interface MultiAgent {

    AgentResult invoke(String prompt);
}
