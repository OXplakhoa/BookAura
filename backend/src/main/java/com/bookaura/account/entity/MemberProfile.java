package com.bookaura.account.entity;

import com.bookaura.auth.entity.UserAccount;
import com.bookaura.loan.entity.Loan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Library-facing member data. Owns the 1-1 FK to UserAccount (member_profiles.user_account_id UNIQUE).
 * Separated so credentials/security concerns stay in the auth module.
 */
@Entity
@Table(name = "member_profiles")
@Getter
@Setter
public class MemberProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(nullable = false, length = 120)
    private String fullName;

    private LocalDate dateOfBirth;

    @Column(length = 255)
    private String address;

    /** Inverse side only; Loan owns the FK. No cascade because loan history must never be deleted. */
    @OneToMany(mappedBy = "memberProfile", fetch = FetchType.LAZY)
    private Set<Loan> loans = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberProfile other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
