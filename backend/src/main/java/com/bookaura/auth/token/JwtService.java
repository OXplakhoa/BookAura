package com.bookaura.auth.token;

import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.common.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Creates and validates short-lived access tokens.
 * Claims: jti (for logout revocation), sub (user UUID), roles, iat, exp.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessTtl;

    public JwtService(JwtProperties properties) {
        byte[] secret = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("bookaura.security.jwt-secret must be >= 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessTtl = Duration.ofMinutes(properties.getAccessTokenTtlMinutes());
    }

    public record IssuedToken(String token, String jti, Instant expiresAt, long expiresInSeconds) {
    }

    public IssuedToken createAccessToken(UserAccount user) {
        // Truncate to seconds: JWT date claims are second-precision; keeps exp claim == expiresAt exactly.
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        Instant exp = now.plus(accessTtl);
        String jti = UUID.randomUUID().toString();
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        String token = Jwts.builder()
                .id(jti)
                .subject(user.getId().toString())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
        return new IssuedToken(token, jti, exp, accessTtl.toSeconds());
    }

    /** Throws io.jsonwebtoken.JwtException on invalid signature/expiry. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    @SuppressWarnings("unchecked")
    public List<String> roles(Claims claims) {
        Object raw = claims.get("roles");
        return raw instanceof List<?> list ? (List<String>) list : List.of();
    }
}
