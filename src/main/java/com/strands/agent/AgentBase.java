package com.strands.agent;

public interface AgentBase {

    AgentResult invoke(String prompt);

    AgentResult invoke(String prompt, com.strands.model.StreamHandler handler);
}
