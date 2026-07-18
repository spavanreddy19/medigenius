package com.medigenius.ai.agents;

import com.medigenius.ai.AgentNode;
import com.medigenius.ai.AgentState;
import org.springframework.stereotype.Component;

/**
 * Direct port of backend/app/agents/explanation.py -> ExplanationAgent.
 * Placeholder node reserved for future explanation/post-processing logic.
 * Currently a pure pass-through, exactly as in the Python original.
 */
@Component
public class ExplanationAgent implements AgentNode {

    @Override
    public AgentState run(AgentState state) {
        return state;
    }

    @Override
    public String name() {
        return "explanation";
    }
}
