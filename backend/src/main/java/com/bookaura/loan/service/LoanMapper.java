package com.bookaura.loan.service;

import com.bookaura.loan.dto.LoanResponse;
import com.bookaura.loan.entity.Loan;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class LoanMapper {

    public LoanResponse toResponse(Loan loan) {
        boolean active = loan.getReturnedAt() == null;
        return new LoanResponse(
                loan.getId(),
                loan.getMemberProfile().getId(),
                loan.getMemberProfile().getUserAccount().getId(),
                loan.getMemberProfile().getFullName(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getBook().getIsbn(),
                loan.getBorrowedAt(),
                loan.getDueAt(),
                loan.getReturnedAt(),
                active ? "ACTIVE" : "RETURNED",
                active && loan.getDueAt().isBefore(Instant.now()));
    }
}
