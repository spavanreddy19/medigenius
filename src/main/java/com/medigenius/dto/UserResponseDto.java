package com.medigenius.dto;

/** NEW DTO (Feature 2/12) - safe, public-facing view of a User (never includes the password). */
public record UserResponseDto(
        Long id,
        String name,
        String email,
        String role,
        String createdAt
) {
}
