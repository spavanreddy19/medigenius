package com.medigenius.repository;

import com.medigenius.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** NEW REPOSITORY (Feature 4 - Chat History). */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findBySessionId(String sessionId);

    List<Conversation> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    void deleteBySessionId(String sessionId);
}
