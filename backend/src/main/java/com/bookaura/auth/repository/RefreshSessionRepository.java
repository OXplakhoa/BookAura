package com.bookaura.auth.repository;

import com.bookaura.auth.entity.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    /** Reuse/theft response: kill every still-active session in the rotation family. */
    @Modifying
    @Query("UPDATE RefreshSession s SET s.revokedAt = :now WHERE s.familyId = :familyId AND s.revokedAt IS NULL")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);
}
