package com.bookaura.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One table for all one-time secrets (verification links, OTP codes, reset tokens).
 * Only the SHA-256 hash is persisted. Enforces: expiry, one-time use, attempt limit,
 * resend cooldown, and a purpose so a token can never be replayed across flows.
 */
@Entity
@Table(name = "otp_tokens", indexes = @Index(name = "idx_otp_lookup", columnList = "code_hash, purpose"))
@Getter
@Setter
public class OtpToken {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Nullable: PHONE_LOGIN may target a phone number with no account yet. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id")
    private UserAccount userAccount;

    /** email / phone / new-email payload this token acts upon. */
    @Column(length = 255)
    private String target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OtpPurpose purpose;

    @Column(nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private Instant expiresAt;

    /** non-null = already used (one-time). */
    private Instant consumedAt;

    @Column(nullable = false)
    private Instant lastSentAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OtpToken other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
