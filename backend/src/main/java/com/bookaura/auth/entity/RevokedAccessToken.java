package com.bookaura.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Logout blacklist: jti of an access token is kept until its natural expiry.
 * JwtAuthenticationFilter rejects any presented token whose jti is found here.
 */
@Entity
@Table(name = "revoked_access_tokens")
@Getter
@Setter
public class RevokedAccessToken {

    @Id
    @Column(length = 36)
    private String jti;

    /** Natural expiry of the access token; row can be cleaned up after this. */
    @Column(nullable = false)
    private Instant expiresAt;
}
