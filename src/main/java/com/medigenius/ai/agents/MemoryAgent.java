package com.medigenius.ai.agents;

import com.medigenius.ai.AgentNode;
import com.medigenius.ai.AgentState;
import com.medigenius.config.MediGeniusProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Direct port of backend/app/agents/memory.py.
 * Trims state.conversation_history down to the last `max_history_turns` entries (default 20)
 * so context sent to the LLM doesn't grow unbounded across a long session.
 */
@Component
@RequiredArgsConstructor
public class MemoryAgent implements AgentNode {

    private final MediGeniusProperties properties;

    @Override
    public AgentState run(AgentState state) {
        List<AgentState.ConversationTurn> history = state.getConversationHistory();
        int maxTurns = properties.getChat().getMaxHistoryTurns();

        if (history.size() > maxTurns) {
            state.setConversationHistory(
                    history.subList(history.size() - maxTurns, history.size())
            );
        }
        return state;
    }

    @Override
    public String name() {
        return "memory";
    }
}
