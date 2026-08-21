package com.bookaura.auth.token;

import com.bookaura.auth.repository.RefreshSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Reuse/theft response in its OWN transaction (REQUIRES_NEW): the rotate() flow throws
 * afterwards, which would roll back the family revocation if it shared the transaction.
 * This split is deliberate - the security response must persist even though the request fails.
 */
@Component
public class RefreshFamilyRevoker {

    private static final Logger AUDIT = LoggerFactory.getLogger("com.bookaura.audit");

    private final RefreshSessionRepository repository;

    public RefreshFamilyRevoker(RefreshSessionRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeFamily(UUID familyId) {
        int revoked = repository.revokeFamily(familyId, Instant.now());
        AUDIT.warn("event=REFRESH_REUSE_DETECTED familyId={} sessionsRevoked={}", familyId, revoked);
    }
}
