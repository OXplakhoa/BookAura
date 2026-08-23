package com.bookaura.member.dto;

import com.bookaura.auth.entity.AccountStatus;

/** Raw web input: dates are strictly parsed by MemberService as yyyy/MM/d. */
public record MemberSearchInput(
        String name,
        String emailOrPhone,
        String dateOfBirthFrom,
        String dateOfBirthTo,
        String borrowedBookTitle,
        AccountStatus status,
        String role,
        Boolean emailVerified
) {
}
