package com.bookaura.auth.repository;

import com.bookaura.auth.entity.OtpPurpose;
import com.bookaura.auth.entity.OtpToken;
import com.bookaura.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    Optional<OtpToken> findByCodeHashAndPurpose(String codeHash, OtpPurpose purpose);

    /** Latest token for cooldown checks and authenticated six-digit confirmation flows. */
    Optional<OtpToken> findTopByUserAccountAndPurposeOrderByLastSentAtDesc(UserAccount userAccount, OtpPurpose purpose);

    @Modifying(flushAutomatically = true)
    @Query("""
            update OtpToken token set token.attempts = token.attempts + 1
            where token.id = :id and token.consumedAt is null and token.attempts < :maxAttempts
            """)
    int incrementAttempt(@Param("id") UUID id, @Param("maxAttempts") int maxAttempts);

    @Modifying(flushAutomatically = true)
    @Query("""
            update OtpToken token set token.consumedAt = :now
            where token.id = :id and token.userAccount.id = :userId and token.purpose = :purpose
              and token.codeHash = :codeHash and token.consumedAt is null
              and token.expiresAt > :now and token.attempts < :maxAttempts
            """)
    int consumeIfUsable(@Param("id") UUID id, @Param("userId") UUID userId,
                        @Param("purpose") OtpPurpose purpose, @Param("codeHash") String codeHash,
                        @Param("now") Instant now, @Param("maxAttempts") int maxAttempts);
}
