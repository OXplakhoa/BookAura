package com.bookaura.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Server-side refresh session. Only the SHA-256 hash of the refresh token is stored.
 * Rotation: each refresh revokes this session and creates a new one in the same familyId.
 * Reuse of a revoked token revokes the whole family (theft detection).
 */
@Entity
@Table(name = "refresh_sessions", indexes = @Index(name = "idx_refresh_family", columnList = "family_id"))
@Getter
@Setter
public class RefreshSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private Instant expiresAt;

    /** null = active. */
    private Instant revokedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 45)
    private String ip;

    private String userAgent;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshSession other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
