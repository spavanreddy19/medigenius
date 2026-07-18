package com.medigenius.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity - direct 1:1 port of backend/app/models/message.py (SQLAlchemy Message).
 *
 * Python:
 *   id            Integer, PK, autoincrement
 *   session_id    String(255), not null, indexed
 *   role          String(50), not null       ("user" | "assistant")
 *   content       Text, not null
 *   source        String(255), nullable      (e.g. "Wikipedia Medical Information")
 *   timestamp     DateTime, default utcnow, not null
 *
 * MODIFIED (Feature 4/5 - Chat History + Memory): added nullable userId/conversationId.
 * Both are plain FK-id columns (not @ManyToOne) on purpose, matching this class's existing
 * simple style, and both stay NULL for anonymous (non-logged-in) chats exactly like before -
 * this is purely additive and does not change any existing query or API response shape.
 */
@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_session_id", columnList = "session_id"),
        @Index(name = "idx_messages_user_id", columnList = "user_id"),
        @Index(name = "idx_messages_conversation_id", columnList = "conversation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "source", length = 255)
    private String source;

    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /** NEW (nullable) - id of the User who owns this message, if the sender was logged in. */
    @Column(name = "user_id")
    private Long userId;

    /** NEW (nullable) - id of the Conversation this message belongs to, if any. */
    @Column(name = "conversation_id")
    private Long conversationId;
}
