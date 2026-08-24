package com.bookaura.auth.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
            String rawCode = dispatch(authentication);
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

    private String dispatch(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            throw new IllegalStateException("Unexpected OAuth authentication type");
        }
        return switch (token.getAuthorizedClientRegistrationId()) {
            case "google" -> beginGoogle(token.getPrincipal());
            case "facebook" -> beginFacebook(token.getPrincipal());
            default -> throw new IllegalStateException("Unsupported OAuth provider");
        };
    }

    /** Google is OIDC: principal carries a provider-signed ID token with verified claims. */
    private String beginGoogle(OAuth2User principal) {
        if (!(principal instanceof OidcUser user)) {
            throw new IllegalStateException("Google principal is not OIDC");
        }
        boolean verified = Boolean.TRUE.equals(user.getClaim("email_verified"));
        return loginService.beginGoogleLogin(new OAuthLoginService.GoogleClaims(
                user.getSubject(), user.getEmail(), verified, user.getFullName()));
    }

    /** Facebook is classic OAuth2: attributes come from Graph API /me (id, name, optional email). */
    private String beginFacebook(OAuth2User user) {
        return loginService.beginFacebookLogin(new OAuthLoginService.FacebookClaims(
                user.getAttribute("id"), user.getAttribute("email"), user.getAttribute("name")));
    }

    private String callbackUrl(String parameter, String value) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .pathSegment("oauth", "callback")
                .queryParam(parameter, value)
                .build().encode().toUriString();
    }
}
