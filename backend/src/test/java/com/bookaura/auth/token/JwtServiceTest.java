package com.bookaura.auth.token;

import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.common.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-long-enough-for-hs256-0123456789";

    private JwtService serviceWithTtl(int minutes) {
        JwtProperties props = new JwtProperties();
        props.setJwtSecret(SECRET);
        props.setAccessTokenTtlMinutes(minutes);
        return new JwtService(props);
    }

    private UserAccount user() {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setEmail("a@b.dev");
        Role role = new Role();
        role.setId(1L);
        role.setName(Role.USER);
        user.getRoles().add(role);
        return user;
    }

    @Test
    void accessToken_containsJtiSubjectRolesAndExpiry() {
        JwtService.IssuedToken issued = serviceWithTtl(15).createAccessToken(user());
        Claims claims = serviceWithTtl(15).parse(issued.token());

        assertThat(claims.getId()).isEqualTo(issued.jti());
        assertThat(claims.getSubject()).isNotBlank();
        assertThat(claims.get("roles", java.util.List.class)).containsExactly("USER");
        assertThat(claims.getExpiration().toInstant()).isEqualTo(issued.expiresAt());
    }

    @Test
    void tamperedToken_isRejected() {
        String token = serviceWithTtl(15).createAccessToken(user()).token();
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThatThrownBy(() -> serviceWithTtl(15).parse(tampered))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void alreadyExpiredToken_isRejected() {
        // Negative TTL => token is born expired; parse must fail immediately.
        String token = serviceWithTtl(-1).createAccessToken(user()).token();
        assertThatThrownBy(() -> serviceWithTtl(-1).parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tooShortSecret_failsFastAtStartup() {
        JwtProperties props = new JwtProperties();
        props.setJwtSecret("short");
        assertThatThrownBy(() -> new JwtService(props))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
