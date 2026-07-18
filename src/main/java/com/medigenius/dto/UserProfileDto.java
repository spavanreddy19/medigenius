package com.medigenius.dto;

import java.util.List;

/** NEW DTO (Feature 12 - Profile). GET /api/users/me response body. */
public record UserProfileDto(
        Long id,
        String name,
        String email,
        String role,
        String createdAt,
        long totalConversations,
        List<UploadedDocumentDto> uploadedDocuments
) {
}
