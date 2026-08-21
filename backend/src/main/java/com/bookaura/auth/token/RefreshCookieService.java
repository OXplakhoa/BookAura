package com.bookaura.auth.token;

import com.bookaura.common.security.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * The refresh token travels ONLY in this cookie: HttpOnly (JS-invisible), SameSite=Lax,
 * Secure in non-local profiles, Path restricted to /api/auth so it is never sent elsewhere.
 */
@Service
public class RefreshCookieService {

    private final JwtProperties properties;

    public RefreshCookieService(JwtProperties properties) {
        this.properties = properties;
    }

    public void setCookie(HttpServletResponse response, String rawToken, Duration ttl) {
        ResponseCookie cookie = base(rawToken)
                .maxAge(ttl)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearCookie(HttpServletResponse response) {
        ResponseCookie cookie = base("")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public Optional<String> readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> properties.getRefreshCookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> !v.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.getRefreshCookieName(), value)
                .httpOnly(true)
                .secure(properties.isRefreshCookieSecure())
                .sameSite("Lax")
                .path("/api/auth");
    }
}
