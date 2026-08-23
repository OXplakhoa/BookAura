package com.bookaura.auth.oauth;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OAuthExchangeCodeRepository extends JpaRepository<OAuthExchangeCode, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OAuthExchangeCode code set code.consumedAt = :now
            where code.codeHash = :codeHash and code.consumedAt is null and code.expiresAt > :now
            """)
    int consumeIfUsable(@Param("codeHash") String codeHash, @Param("now") Instant now);

    @EntityGraph(attributePaths = {"userAccount.roles", "userAccount.profile"})
    Optional<OAuthExchangeCode> findByCodeHash(String codeHash);
}
