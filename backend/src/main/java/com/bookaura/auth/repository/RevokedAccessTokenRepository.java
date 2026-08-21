package com.bookaura.auth.repository;

import com.bookaura.auth.entity.RevokedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, String> {

    /** Housekeeping: rows are useless after the access token's natural expiry. */
    @Modifying
    @Query("DELETE FROM RevokedAccessToken r WHERE r.expiresAt < :now")
    int deleteExpiredBefore(@Param("now") Instant now);
}
