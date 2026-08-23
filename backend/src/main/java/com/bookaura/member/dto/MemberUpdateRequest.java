package com.bookaura.member.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Email and roles are deliberately excluded: email uses the verified change-email flow; role changes need a separate API. */
public record MemberUpdateRequest(
        @NotBlank @Size(max = 120) String fullName,
        /** blank clears phone; null also means no phone under PUT replacement semantics. */
        @Pattern(regexp = "^$|^\\+?[0-9\\s\\-.]{8,17}$", message = "Phone must be blank or 8-15 digits, optional leading +")
        String phone,
        @Past LocalDate dateOfBirth,
        @Size(max = 255) String address,
        @NotNull Boolean active
) {
}
