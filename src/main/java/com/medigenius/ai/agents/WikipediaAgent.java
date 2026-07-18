package com.medigenius.ai.agents;

import com.medigenius.ai.AgentNode;
import com.medigenius.ai.AgentState;
import com.medigenius.ai.WikipediaClient;
import com.medigenius.config.MediGeniusProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Direct port of backend/app/agents/wikipedia.py -> WikipediaAgent.
 * Fallback used when both retriever and llm_agent fail. Considered successful only if
 * the combined extract content exceeds `medigenius.wikipedia.min-valid-chars` (100).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikipediaAgent implements AgentNode {

    private final WikipediaClient wikipediaClient;
    private final MediGeniusProperties properties;

    @Override
    public AgentState run(AgentState state) {
        state.setWikipediaAttempted(true);
        try {
            List<String> results = wikipediaClient.search(state.getQuestion());
            String combined = String.join("\n\n", results);

            if (combined.length() > properties.getWikipedia().getMinValidChars()) {
                state.setDocuments(results);
                state.setWikipediaSuccess(true);
                state.setSource("Wikipedia Medical Information");
            } else {
                state.setWikipediaSuccess(false);
            }
        } catch (Exception e) {
            log.error("WikipediaAgent failed: {}", e.getMessage(), e);
            state.setWikipediaSuccess(false);
        }
        return state;
    }

    @Override
    public String name() {
        return "wikipedia";
    }
}
