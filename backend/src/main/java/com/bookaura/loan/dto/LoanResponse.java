package com.bookaura.loan.dto;

import java.time.Instant;
import java.util.UUID;

public record LoanResponse(
        UUID id,
        UUID memberId,
        UUID userAccountId,
        String memberName,
        UUID bookId,
        String bookTitle,
        String isbn,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt,
        String status,
        boolean overdue
) {
}
