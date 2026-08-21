package com.bookaura.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,

        @NotBlank @Email @Size(max = 255) String email,

        /** Optional; normalized before storage. */
        @Pattern(regexp = "^\\+?[0-9\\s\\-.]{8,17}$", message = "Phone must be 8-15 digits, optional leading +")
        String phone,

        /** Password policy: 8-72 chars (72 = BCrypt limit), at least one letter and one digit. */
        @NotBlank
        @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one digit")
        String password
) {
}
