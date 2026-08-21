package com.bookaura.auth.token;

import com.bookaura.auth.entity.RefreshSession;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.RefreshSessionRepository;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.common.security.JwtProperties;
import com.bookaura.common.util.HashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshSessionRepository repository;

    @Mock
    private RefreshFamilyRevoker familyRevoker;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setRefreshTokenTtlDays(7);
        service = new RefreshTokenService(repository, props, familyRevoker);
    }

    private RefreshSession activeSession(String rawToken) {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        RefreshSession session = new RefreshSession();
        session.setId(UUID.randomUUID());
        session.setUserAccount(user);
        session.setTokenHash(HashUtils.sha256Hex(rawToken));
        session.setFamilyId(UUID.randomUUID());
        session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        return session;
    }

    @Test
    void rotate_activeToken_revokesOldAndIssuesNewInSameFamily() {
        RefreshSession old = activeSession("raw-old");
        when(repository.findByTokenHash(HashUtils.sha256Hex("raw-old"))).thenReturn(Optional.of(old));

        RefreshTokenService.RotationResult result = service.rotate("raw-old", "127.0.0.1", "agent");

        assertThat(old.getRevokedAt()).isNotNull();
        ArgumentCaptor<RefreshSession> saved = ArgumentCaptor.forClass(RefreshSession.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getFamilyId()).isEqualTo(old.getFamilyId());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(HashUtils.sha256Hex(result.session().rawToken()));
    }

    @Test
    void rotate_revokedToken_triggersReuseDetection_andRevokesFamily() {
        RefreshSession old = activeSession("raw-old");
        old.setRevokedAt(Instant.now()); // already rotated once -> presenting it again = reuse
        when(repository.findByTokenHash(HashUtils.sha256Hex("raw-old"))).thenReturn(Optional.of(old));

        assertThatThrownBy(() -> service.rotate("raw-old", "127.0.0.1", "agent"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.REFRESH_REUSED);
        // Family revocation must go through the REQUIRES_NEW revoker so it survives the rollback
        verify(familyRevoker).revokeFamily(old.getFamilyId());
        verify(repository, never()).save(any());
    }

    @Test
    void rotate_expiredToken_failsWithoutRotation() {
        RefreshSession old = activeSession("raw-old");
        old.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(repository.findByTokenHash(HashUtils.sha256Hex("raw-old"))).thenReturn(Optional.of(old));

        assertThatThrownBy(() -> service.rotate("raw-old", "127.0.0.1", "agent"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.REFRESH_EXPIRED);
        verify(repository, never()).save(any());
    }

    @Test
    void rotate_unknownToken_fails() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rotate("nope", "127.0.0.1", "agent"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.REFRESH_INVALID);
    }
}
