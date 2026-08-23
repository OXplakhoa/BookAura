package com.bookaura.member.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record MemberCreateRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @Pattern(regexp = "^\\+?[0-9\\s\\-.]{8,17}$", message = "Phone must be 8-15 digits, optional leading +")
        String phone,
        @NotBlank
        @Size(min = 8, max = 72, message = "Initial password must be 8-72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Initial password must contain at least one letter and one digit")
        String initialPassword,
        @Past LocalDate dateOfBirth,
        @Size(max = 255) String address,
        /** Default false: ADMIN may mark verified after an in-person identity check. */
        Boolean emailVerified,
        /** Default true. */
        Boolean active
) {
}
