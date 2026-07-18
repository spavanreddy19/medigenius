package com.medigenius.ai.agents;

import com.medigenius.ai.AgentNode;
import com.medigenius.ai.AgentState;
import com.medigenius.config.MediGeniusProperties;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Direct port of backend/app/agents/executor.py -> ExecutorAgent.
 * Always runs last. Synthesizes the final patient-facing answer from whichever
 * upstream node succeeded (documents from retriever/wikipedia/tavily, or a
 * pre-generated llm_agent response), or falls back to a canned "consult a
 * professional" message if nothing succeeded. Appends both turns to history.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutorAgent implements AgentNode {

    private static final String SYSTEM_UNAVAILABLE_MESSAGE =
            "Medical AI service temporarily unavailable. Please consult a healthcare professional.";

    private static final String CONSULT_PROFESSIONAL_MESSAGE =
            "I understand your concern about your symptoms. For accurate medical advice, "
                    + "please consult with a healthcare professional who can properly evaluate your condition.";

    private final ChatLanguageModel chatLanguageModel;
    private final MediGeniusProperties properties;

    @Override
    public AgentState run(AgentState state) {
        String question = state.getQuestion();
        String sourceInfo = state.getSource() != null ? state.getSource() : "Unknown";

        // Build recent conversation context (last 3 turns), same as Python's [-3:] slice.
        StringBuilder historyContext = new StringBuilder();
        List<AgentState.ConversationTurn> history = state.getConversationHistory();
        int from = Math.max(0, history.size() - properties.getChat().getExecutorContextWindow());
        for (AgentState.ConversationTurn turn : history.subList(from, history.size())) {
            if ("user".equalsIgnoreCase(turn.getRole())) {
                historyContext.append("Patient: ").append(turn.getContent()).append("\n");
            } else if ("assistant".equalsIgnoreCase(turn.getRole())) {
                historyContext.append("Doctor: ").append(turn.getContent()).append("\n");
            }
        }

        String answer;

        if (chatLanguageModel == null) {
            // Defensive parity with Python's `if not llm:` guard - not normally reachable
            // since Spring DI guarantees the bean, but kept for behavioral fidelity.
            answer = SYSTEM_UNAVAILABLE_MESSAGE;
            sourceInfo = "System Message";
        } else if (state.getDocuments() != null && !state.getDocuments().isEmpty()) {
            // Take up to 3 documents, each truncated to 1000 chars (mirrors doc.page_content[:1000]).
            String content = state.getDocuments().stream()
                    .limit(3)
                    .map(doc -> doc.length() > 1000 ? doc.substring(0, 1000) : doc)
                    .reduce((a, b) -> a + "\n\n" + b)
                    .orElse("");

            String prompt = "You are an experienced medical doctor providing helpful consultation.\n\n"
                    + "Previous Conversation:\n" + historyContext + "\n"
                    + "Patient's Current Question:\n" + question + "\n\n"
                    + "Medical Information:\n" + content + "\n\n"
                    + "Provide a clear, caring response in 2-4 sentences. Be professional and reassuring.";

            try {
                String response = chatLanguageModel.generate(UserMessage.from(prompt)).content().text();
                answer = response == null ? "" : response.strip();
                log.info("Executor: Generated response from documents");
            } catch (Exception e) {
                log.error("Executor: LLM generation failed: {}", e.getMessage(), e);
                answer = CONSULT_PROFESSIONAL_MESSAGE;
                sourceInfo = "System Message";
            }
        } else if (state.isLlmSuccess() && state.getGeneration() != null) {
            answer = state.getGeneration();
            log.info("Executor: Using pre-generated LLM response");
        } else {
            answer = CONSULT_PROFESSIONAL_MESSAGE;
            sourceInfo = "System Message";
        }

        state.setGeneration(answer);
        state.setFinalAnswer(answer);
        state.setSource(sourceInfo);
        state.getConversationHistory().add(new AgentState.ConversationTurn("user", question));
        state.getConversationHistory().add(new AgentState.ConversationTurn("assistant", answer, sourceInfo));
        return state;
    }

    @Override
    public String name() {
        return "executor";
    }
}
