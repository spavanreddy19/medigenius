package com.medigenius.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** NEW DTO (Feature 1) - POST /api/auth/register request body. */
public record RegisterRequestDto(
        @NotBlank(message = "name must not be blank")
        String name,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "password must not be blank")
        @Size(min = 6, message = "password must be at least 6 characters")
        String password
) {
}
