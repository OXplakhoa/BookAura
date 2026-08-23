package com.bookaura.member.specification;

import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.catalog.entity.Book;
import com.bookaura.loan.entity.Loan;
import com.bookaura.member.dto.MemberSearchCriteria;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Seven independently composable member filters; all execute in SQL. */
public final class MemberSpecifications {

    private MemberSpecifications() {
    }

    public static Specification<MemberProfile> from(MemberSearchCriteria c) {
        Specification<MemberProfile> spec = Specification.unrestricted();
        if (hasText(c.name())) spec = spec.and(nameContains(c.name()));
        if (hasText(c.emailOrPhone())) spec = spec.and(emailOrPhoneContains(c.emailOrPhone()));
        if (c.dateOfBirthFrom() != null) spec = spec.and(dateOfBirthFrom(c.dateOfBirthFrom()));
        if (c.dateOfBirthTo() != null) spec = spec.and(dateOfBirthTo(c.dateOfBirthTo()));
        if (hasText(c.borrowedBookTitle())) spec = spec.and(borrowedBookTitleContains(c.borrowedBookTitle()));
        if (c.status() != null) spec = spec.and(status(c.status()));
        if (hasText(c.role())) spec = spec.and(role(c.role()));
        if (c.emailVerified() != null) spec = spec.and(emailVerified(c.emailVerified()));
        return spec;
    }

    private static Specification<MemberProfile> nameContains(String value) {
        String pattern = pattern(value);
        return (root, query, cb) -> cb.like(cb.lower(root.get("fullName")), pattern, '\\');
    }

    private static Specification<MemberProfile> emailOrPhoneContains(String value) {
        String pattern = pattern(value);
        String phonePattern = "%" + value.trim().replaceAll("[\\s\\-.]", "") + "%";
        return (root, query, cb) -> {
            Join<MemberProfile, UserAccount> user = root.join("userAccount", JoinType.INNER);
            return cb.or(
                    cb.like(cb.lower(user.get("email")), pattern, '\\'),
                    cb.like(user.get("phone"), phonePattern));
        };
    }

    private static Specification<MemberProfile> dateOfBirthFrom(java.time.LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dateOfBirth"), from);
    }

    private static Specification<MemberProfile> dateOfBirthTo(java.time.LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dateOfBirth"), to);
    }

    private static Specification<MemberProfile> borrowedBookTitleContains(String value) {
        String pattern = pattern(value);
        return (root, query, cb) -> {
            // EXISTS avoids duplicate member rows when the same title was borrowed multiple times.
            // This also avoids PostgreSQL's DISTINCT + nested ORDER BY restriction during pagination.
            var subquery = query.subquery(Integer.class);
            var loan = subquery.from(Loan.class);
            Join<Loan, Book> book = loan.join("book", JoinType.INNER);
            subquery.select(cb.literal(1));
            subquery.where(
                    cb.equal(loan.get("memberProfile").get("id"), root.get("id")),
                    cb.like(cb.lower(book.get("title")), pattern, '\\'));
            return cb.exists(subquery);
        };
    }

    private static Specification<MemberProfile> status(AccountStatus status) {
        return (root, query, cb) -> cb.equal(root.join("userAccount").get("status"), status);
    }

    private static Specification<MemberProfile> role(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return (root, query, cb) -> {
            Join<MemberProfile, UserAccount> user = root.join("userAccount", JoinType.INNER);
            Join<UserAccount, Role> roles = user.join("roles", JoinType.INNER);
            // Exact role name + join-table composite PK yields at most one matching row per member.
            return cb.equal(roles.get("name"), normalized);
        };
    }

    private static Specification<MemberProfile> emailVerified(boolean verified) {
        return (root, query, cb) -> {
            var path = root.join("userAccount", JoinType.INNER).get("emailVerifiedAt");
            return verified ? cb.isNotNull(path) : cb.isNull(path);
        };
    }

    private static String pattern(String value) {
        return "%" + escapeLike(value.trim().toLowerCase(Locale.ROOT)) + "%";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
