package com.medigenius.service;

import com.medigenius.ai.AgentState;
import com.medigenius.ai.WorkflowEngine;
import com.medigenius.dto.ChatResponseDto;
import com.medigenius.dto.MessageResponseDto;
import com.medigenius.entity.Conversation;
import com.medigenius.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Direct port of backend/app/services/chat_service.py -> ChatService.
 * Orchestrates the agentic workflow for each chat message: persists the user turn,
 * loads/creates the session's in-memory AgentState, runs the WorkflowEngine, persists
 * the assistant turn, and returns the API response shape.
 *
 * MODIFIED (Features 4 & 5 - Chat History + Conversation Memory): processMessage() now has
 * an overload that additionally accepts the (optional) authenticated User. When present,
 * it attaches a titled Conversation to the session (Feature 4) and stamps every Message row
 * with user_id/conversation_id. The original 2-arg signature is preserved and simply
 * delegates with user=null, so every existing (anonymous) call path behaves exactly as before.
 *
 * Feature 5 (Memory): AgentState.conversationHistory was already appended-to by ExecutorAgent,
 * but only lived in the process-local `conversationStates` map, so it was lost on restart.
 * The very first time a session is seen in a given JVM run, we now hydrate it from the
 * `messages` table (already persisted to MySQL) - the same context the LLM had before restart.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private final WorkflowEngine workflowEngine;
    private final DatabaseService databaseService;
    private final ConversationService conversationService;

    /**
     * In-memory per-session conversation state, equivalent of Python's
     * `self.conversation_states: Dict[str, Dict]` on the ChatService singleton.
     * NOTE: like the original, this is process-local (not shared across horizontally
     * scaled instances) - if you need multi-instance deployment, back this with Redis.
     */
    private final Map<String, AgentState> conversationStates = new ConcurrentHashMap<>();

    /** Original signature, preserved for backward compatibility - anonymous chat. */
    public ChatResponseDto processMessage(String sessionId, String message) {
        return processMessage(sessionId, message, null);
    }

    /** NEW OVERLOAD (Features 4 & 5) - same flow, plus optional owning User. */
    public ChatResponseDto processMessage(String sessionId, String message, User user) {
        log.info("Processing message for session {}...", safePrefix(sessionId));

        Long conversationId = null;
        if (user != null) {
            Conversation conversation = conversationService.getOrCreateConversation(user, sessionId, message);
            conversationId = conversation.getId();
        }
        Long userId = user != null ? user.getId() : null;

        // Persist user message
        databaseService.saveMessage(sessionId, "user", message, null, userId, conversationId);

        // Initialize or retrieve conversation state
        AgentState state = conversationStates.computeIfAbsent(sessionId, id -> hydrateFreshState(id));
        state.resetForNewQuestion(message);

        // Run workflow
        AgentState result = workflowEngine.invoke(state);
        conversationStates.put(sessionId, result);

        String responseText = result.getFinalAnswer() != null
                ? result.getFinalAnswer()
                : (result.getGeneration() != null ? result.getGeneration() : "Unable to generate response.");
        String source = result.getSource() != null ? result.getSource() : "Unknown";

        // Persist assistant response
        databaseService.saveMessage(sessionId, "assistant", responseText, source, userId, conversationId);

        boolean success = result.getGeneration() != null && !result.getGeneration().isBlank();

        return new ChatResponseDto(
                responseText,
                source,
                LocalTime.now().format(TIMESTAMP_FORMATTER),
                success
        );
    }

    /**
     * NEW (Feature 5 - Conversation Memory). Builds a fresh AgentState for a session this
     * JVM instance hasn't seen yet, pre-populated with its prior turns already saved in
     * MySQL - so "what did you recommend earlier?" keeps working across app restarts, not
     * just within a single process lifetime.
     */
    private AgentState hydrateFreshState(String sessionId) {
        AgentState fresh = new AgentState();
        fresh.setSessionId(sessionId);

        for (MessageResponseDto m : databaseService.getChatHistory(sessionId)) {
            fresh.getConversationHistory().add(
                    new AgentState.ConversationTurn(m.role(), m.content(), m.source()));
        }
        return fresh;
    }

    /** Equivalent of ChatService.clear_conversation(session_id). */
    public void clearConversation(String sessionId) {
        if (conversationStates.containsKey(sessionId)) {
            AgentState fresh = new AgentState();
            fresh.setSessionId(sessionId);
            conversationStates.put(sessionId, fresh);
            log.info("Conversation cleared for session {}", safePrefix(sessionId));
        }
    }

    private String safePrefix(String sessionId) {
        return sessionId != null && sessionId.length() >= 8 ? sessionId.substring(0, 8) : sessionId;
    }
}
