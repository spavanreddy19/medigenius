package com.medigenius.ai.agents;

import com.medigenius.ai.AgentNode;
import com.medigenius.ai.AgentState;
import com.medigenius.ai.TavilyClient;
import com.medigenius.config.MediGeniusProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Direct port of backend/app/agents/tavily.py -> TavilyAgent.
 * Last-resort fallback (after retriever, llm_agent, and wikipedia all fail).
 * Unlike the other fallback nodes, this one always proceeds to the executor regardless
 * of outcome - the executor node handles the "nothing succeeded" case itself.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TavilyAgent implements AgentNode {

    private final TavilyClient tavilyClient;
    private final MediGeniusProperties properties;

    @Override
    public AgentState run(AgentState state) {
        state.setTavilyAttempted(true);
        try {
            List<String> results = tavilyClient.search(state.getQuestion());
            String combined = String.join("\n\n", results);

            if (combined.length() > properties.getTavily().getMinValidChars()) {
                state.setDocuments(results);
                state.setTavilySuccess(true);
                state.setSource("Web Search Results");
            } else {
                state.setTavilySuccess(false);
            }
        } catch (Exception e) {
            log.error("TavilyAgent failed: {}", e.getMessage(), e);
            state.setTavilySuccess(false);
        }
        return state;
    }

    @Override
    public String name() {
        return "tavily";
    }
}
