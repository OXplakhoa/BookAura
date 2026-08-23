package com.bookaura.member.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        UUID userAccountId,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String address,
        String status,
        boolean emailVerified,
        List<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
}
