package com.medigenius.ai.agents;

import com.medigenius.ai.AgentNode;
import com.medigenius.ai.AgentState;
import com.medigenius.ai.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Direct port of backend/app/agents/retriever.py -> RetrieverAgent.
 * Queries the vector store (k=3) and keeps only documents longer than the configured
 * minimum valid length (default 50 chars), exactly like the Python implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetrieverAgent implements AgentNode {

    private final VectorStoreService vectorStoreService;

    @Override
    public AgentState run(AgentState state) {
        state.setRetrieverAttempted(true);
        try {
            List<String> documents = vectorStoreService.retrieveRelevantDocuments(state.getQuestion());

            if (!documents.isEmpty()) {
                state.setDocuments(documents);
                state.setRetrieverSuccess(true);
                state.setSource("Medical Knowledge Base");
            } else {
                state.setRetrieverSuccess(false);
            }
        } catch (Exception e) {
            log.error("RetrieverAgent failed: {}", e.getMessage(), e);
            state.setRetrieverSuccess(false);
        }
        return state;
    }

    @Override
    public String name() {
        return "retriever";
    }
}
