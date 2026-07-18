package com.medigenius.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** NEW DTO (Feature 1) - POST /api/auth/login request body. */
public record LoginRequestDto(
        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "password must not be blank")
        String password
) {
}
