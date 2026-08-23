package com.bookaura.auth.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuthLoginSuccessHandler.class);

    private final OAuthLoginService loginService;
    private final String frontendUrl;

    public OAuthLoginSuccessHandler(OAuthLoginService loginService,
                                    @Value("${bookaura.frontend-url}") String frontendUrl) {
        this.loginService = loginService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            if (!(authentication.getPrincipal() instanceof OidcUser user)) {
                throw new IllegalStateException("Google principal is not OIDC");
            }
            boolean verified = Boolean.TRUE.equals(user.getClaim("email_verified"));
            String rawCode = loginService.beginGoogleLogin(new OAuthLoginService.GoogleClaims(
                    user.getSubject(), user.getEmail(), verified, user.getFullName()));
            // OAuth authorization needs transient server-side state; destroy that handshake session immediately.
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
            response.sendRedirect(callbackUrl("code", rawCode));
        } catch (Exception exception) {
            // Never redirect exception details, provider claims, email, code, or tokens.
            log.warn("OAuth callback failed exception={}", exception.getClass().getSimpleName());
            response.sendRedirect(callbackUrl("error", "oauth_failed"));
        }
    }

    private String callbackUrl(String parameter, String value) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .pathSegment("oauth", "callback")
                .queryParam(parameter, value)
                .build().encode().toUriString();
    }
}
