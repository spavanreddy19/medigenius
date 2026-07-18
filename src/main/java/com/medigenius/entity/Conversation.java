package com.medigenius.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * NEW ENTITY (Feature 4 - Chat History).
 *
 * A titled, user-owned wrapper around an existing chat "session" (see SessionIdService /
 * Message.sessionId, both untouched). sessionId here is the SAME value already used by
 * Message.sessionId - this table simply adds a title + owner on top of it for logged-in
 * users. Anonymous chats (no JWT) never get a Conversation row, so nothing existing breaks.
 */
@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conversations_session_id", columnList = "session_id", unique = true),
        @Index(name = "idx_conversations_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Same value as Message.sessionId - the join key between the two tables. */
    @Column(name = "session_id", nullable = false, unique = true, length = 255)
    private String sessionId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
