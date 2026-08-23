package com.bookaura.member.dto;

import com.bookaura.auth.entity.AccountStatus;

import java.time.LocalDate;

public record MemberSearchCriteria(
        String name,
        String emailOrPhone,
        LocalDate dateOfBirthFrom,
        LocalDate dateOfBirthTo,
        String borrowedBookTitle,
        AccountStatus status,
        String role,
        Boolean emailVerified
) {
}
