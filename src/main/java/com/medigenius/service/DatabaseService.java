package com.medigenius.service;

import com.medigenius.dto.MessageResponseDto;
import com.medigenius.dto.SessionResponseDto;
import com.medigenius.entity.Message;
import com.medigenius.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Direct port of backend/app/services/database_service.py -> DatabaseService.
 * All CRUD operations for chat history, backed by Spring Data JPA / MySQL instead
 * of SQLAlchemy / SQLite.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseService {

    private final MessageRepository messageRepository;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** Equivalent of db_service.save_message(session_id, role, content, source). */
    @Transactional
    public void saveMessage(String sessionId, String role, String content, String source) {
        saveMessage(sessionId, role, content, source, null, null);
    }

    /**
     * NEW OVERLOAD (Feature 4/5 - Chat History + Memory). Same as above, but additionally
     * stamps the owning user/conversation when the sender was logged in. Both new params
     * are nullable and default to null via the overload above, so every existing call site
     * (anonymous chats) is completely unaffected.
     */
    @Transactional
    public void saveMessage(String sessionId, String role, String content, String source,
                             Long userId, Long conversationId) {
        log.debug("Saving {} message for session {}...", role, safePrefix(sessionId));
        Message message = Message.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .source(source)
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .conversationId(conversationId)
                .build();
        messageRepository.save(message);
    }

    /** Equivalent of db_service.get_chat_history(session_id). */
    @Transactional(readOnly = true)
    public List<MessageResponseDto> getChatHistory(String sessionId) {
        return messageRepository.findBySessionIdOrderByTimestampAsc(sessionId).stream()
                .map(m -> new MessageResponseDto(
                        m.getRole(),
                        m.getContent(),
                        m.getSource(),
                        m.getTimestamp().format(ISO_FORMATTER)))
                .toList();
    }

    /**
     * Equivalent of db_service.get_all_sessions(): one preview row per session,
     * truncated to 50 chars + "..." exactly like Python's `content[:50] + "..."`.
     */
    @Transactional(readOnly = true)
    public List<SessionResponseDto> getAllSessions() {
        return messageRepository.findAllSessionPreviews().stream()
                .map(row -> new SessionResponseDto(
                        row.getSessionId(),
                        truncatePreview(row.getPreview()),
                        row.getLastActive() != null
                                ? row.getLastActive().toLocalDateTime().format(ISO_FORMATTER)
                                : null))
                .toList();
    }

    /** Equivalent of db_service.delete_session(session_id). */
    @Transactional
    public void deleteSession(String sessionId) {
        log.info("Deleting session {}...", safePrefix(sessionId));
        messageRepository.deleteBySessionId(sessionId);
    }

    private String truncatePreview(String content) {
        if (content == null) {
            return null;
        }
        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }

    private String safePrefix(String sessionId) {
        return sessionId != null && sessionId.length() >= 8 ? sessionId.substring(0, 8) : sessionId;
    }
}
