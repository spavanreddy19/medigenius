package com.medigenius.ai.agents;

import com.medigenius.ai.AgentNode;
import com.medigenius.ai.AgentState;
import com.medigenius.config.MediGeniusProperties;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct port of backend/app/agents/llm_agent.py -> LLMAgent.
 * Calls the LLM directly (no retrieval), using the last `history_context_window` (5)
 * conversation turns as context. Used both as the primary path (non-medical questions)
 * and as a fallback path when the retriever fails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmAgent implements AgentNode {

    private final ChatLanguageModel chatLanguageModel;
    private final MediGeniusProperties properties;

    @Override
    public AgentState run(AgentState state) {
        state.setLlmAttempted(true);
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(
                    "You are a helpful medical information assistant. Answer clearly and concisely."));

            int window = properties.getChat().getHistoryContextWindow();
            List<AgentState.ConversationTurn> history = state.getConversationHistory();
            int from = Math.max(0, history.size() - window);
            for (AgentState.ConversationTurn turn : history.subList(from, history.size())) {
                if ("user".equalsIgnoreCase(turn.getRole())) {
                    messages.add(UserMessage.from(turn.getContent()));
                } else {
                    messages.add(AiMessage.from(turn.getContent()));
                }
            }
            messages.add(UserMessage.from(state.getQuestion()));

            String answer = chatLanguageModel.generate(messages).content().text();

            if (answer != null && !answer.isBlank()) {
                state.setGeneration(answer);
                state.setLlmSuccess(true);
                state.setSource("AI Language Model");
            } else {
                state.setLlmSuccess(false);
            }
        } catch (Exception e) {
            log.error("LlmAgent failed: {}", e.getMessage(), e);
            state.setLlmSuccess(false);
        }
        return state;
    }

    @Override
    public String name() {
        return "llm_agent";
    }
}
