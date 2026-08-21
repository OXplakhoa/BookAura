package com.bookaura.auth.token;

import com.bookaura.auth.entity.RefreshSession;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.RefreshSessionRepository;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.common.security.JwtProperties;
import com.bookaura.common.util.HashUtils;
import com.bookaura.common.util.TokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Rotating refresh sessions. Raw tokens are random and only ever live in the HttpOnly cookie;
 * the DB stores SHA-256 hashes. Rotation keeps a familyId lineage; presenting a revoked token
 * means theft -> the whole family is revoked.
 */
@Service
public class RefreshTokenService {

    private final RefreshSessionRepository repository;
    private final RefreshFamilyRevoker familyRevoker;
    private final Duration refreshTtl;

    public RefreshTokenService(RefreshSessionRepository repository, JwtProperties properties,
                               RefreshFamilyRevoker familyRevoker) {
        this.repository = repository;
        this.refreshTtl = Duration.ofDays(properties.getRefreshTokenTtlDays());
        this.familyRevoker = familyRevoker;
    }

    public record IssuedSession(String rawToken, Instant expiresAt) {
    }

    @Transactional
    public IssuedSession issue(UserAccount user, String ip, String userAgent) {
        return createSession(user, UUID.randomUUID(), ip, userAgent);
    }

    /**
     * Rotate: old session is revoked, a new one in the same family is issued.
     * Reuse of a revoked token revokes the entire family and fails with 401.
     */
    @Transactional
    public RotationResult rotate(String rawToken, String ip, String userAgent) {
        Instant now = Instant.now();
        RefreshSession session = repository.findByTokenHash(HashUtils.sha256Hex(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_INVALID, "Refresh token not recognized"));

        if (session.getRevokedAt() != null) {
            // REQUIRES_NEW: survives the rollback caused by the exception below.
            familyRevoker.revokeFamily(session.getFamilyId());
            throw new BusinessException(ErrorCode.REFRESH_REUSED, "Refresh token reuse detected; session family revoked");
        }
        if (!session.getExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.REFRESH_EXPIRED, "Refresh token expired");
        }

        session.setRevokedAt(now);
        IssuedSession next = createSession(session.getUserAccount(), session.getFamilyId(), ip, userAgent);
        return new RotationResult(next, session.getUserAccount());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findByTokenHash(HashUtils.sha256Hex(rawToken)).ifPresent(session -> {
            if (session.getRevokedAt() == null) {
                session.setRevokedAt(Instant.now());
            }
        });
    }

    public record RotationResult(IssuedSession session, UserAccount userAccount) {
    }

    private IssuedSession createSession(UserAccount user, UUID familyId, String ip, String userAgent) {
        String raw = TokenGenerator.urlSafeToken();
        RefreshSession session = new RefreshSession();
        session.setUserAccount(user);
        session.setFamilyId(familyId);
        session.setTokenHash(HashUtils.sha256Hex(raw));
        session.setExpiresAt(Instant.now().plus(refreshTtl));
        session.setIp(ip);
        session.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 255)));
        repository.save(session);
        return new IssuedSession(raw, session.getExpiresAt());
    }
}
