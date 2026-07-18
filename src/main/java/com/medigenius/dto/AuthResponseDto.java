package com.medigenius.dto;

/** NEW DTO (Feature 1) - response body for both /api/auth/register and /api/auth/login. */
public record AuthResponseDto(
        String token,
        String tokenType,
        UserResponseDto user
) {
    public static AuthResponseDto of(String token, UserResponseDto user) {
        return new AuthResponseDto(token, "Bearer", user);
    }
}
