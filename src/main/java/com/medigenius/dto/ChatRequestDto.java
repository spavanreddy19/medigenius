package com.medigenius.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Mirrors backend/app/schemas/chat.py -> ChatRequest.
 * POST /api/v1/chat request body: {"message": "..."}
 */
public record ChatRequestDto(
        @NotBlank(message = "message must not be blank")
        String message
) {
}
