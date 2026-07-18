package com.medigenius.dto;

/** NEW DTO (Feature 4/11 - Chat History / Sidebar). */
public record ConversationDto(
        Long id,
        String sessionId,
        String title,
        String createdAt
) {
}
