package com.bookaura.common.security;

import com.bookaura.auth.repository.RevokedAccessTokenRepository;
import com.bookaura.auth.token.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests carrying "Authorization: Bearer <accessToken>".
 * Failures do NOT throw here: the request continues unauthenticated and the
 * AuthenticationEntryPoint produces the consistent 401 (with a specific code from
 * the ATTR_JWT_ERROR attribute). This keeps 401 (who are you?) vs 403 (known but
 * not allowed) semantics clean.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATTR_JWT_ERROR = "bookaura.jwt.error";

    private final JwtService jwtService;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    public JwtAuthenticationFilter(JwtService jwtService, RevokedAccessTokenRepository revokedAccessTokenRepository) {
        this.jwtService = jwtService;
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = jwtService.parse(header.substring(7));
            if (revokedAccessTokenRepository.existsById(claims.getId())) {
                // Logged-out token presented again -> reject (D4).
                request.setAttribute(ATTR_JWT_ERROR, "TOKEN_REVOKED");
            } else {
                List<SimpleGrantedAuthority> authorities = jwtService.roles(claims).stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                // principal = user UUID string; authorities derived from JWT roles claim
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException e) {
            request.setAttribute(ATTR_JWT_ERROR, "TOKEN_INVALID");
        }
        chain.doFilter(request, response);
    }
}
