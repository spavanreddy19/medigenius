package com.medigenius.service;

import com.medigenius.dto.ConversationDto;
import com.medigenius.entity.Conversation;
import com.medigenius.entity.User;
import com.medigenius.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * NEW SERVICE (Feature 4 - Chat History / Feature 11 - Sidebar).
 * Bridges the pre-existing anonymous "session id" concept (SessionIdService, Message.sessionId)
 * with real user accounts by attaching a titled, owned {@link Conversation} row to a session
 * the first time a logged-in user sends a message on it. Purely additive - anonymous chats
 * (no authenticated user) never touch this class.
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int TITLE_MAX_LEN = 60;

    private final ConversationRepository conversationRepository;

    /**
     * Finds the Conversation already backing this sessionId, or creates one titled from the
     * first message, owned by {@code user}. Called once per turn from ChatService; cheap
     * after the first call since it's a unique-indexed lookup.
     */
    @Transactional
    public Conversation getOrCreateConversation(User user, String sessionId, String firstMessage) {
        return conversationRepository.findBySessionId(sessionId)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .user(user)
                                .sessionId(sessionId)
                                .title(buildTitle(firstMessage))
                                .build()));
    }

    /** GET /api/conversations - equivalent of the sidebar's "Conversation History" list. */
    @Transactional(readOnly = true)
    public List<ConversationDto> listForUser(Long userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(c -> new ConversationDto(
                        c.getId(),
                        c.getSessionId(),
                        c.getTitle(),
                        c.getCreatedAt().format(ISO_FORMATTER)))
                .toList();
    }

    @Transactional
    public void deleteBySessionId(String sessionId) {
        conversationRepository.deleteBySessionId(sessionId);
    }

    private String buildTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.isBlank()) {
            return "New conversation";
        }
        String trimmed = firstMessage.trim();
        return trimmed.length() > TITLE_MAX_LEN ? trimmed.substring(0, TITLE_MAX_LEN) + "..." : trimmed;
    }
}
