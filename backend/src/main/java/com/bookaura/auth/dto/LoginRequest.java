package com.bookaura.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** identifier = email (contains '@') or normalized phone number. */
public record LoginRequest(
        @NotBlank String identifier,
        @NotBlank String password
) {
}
