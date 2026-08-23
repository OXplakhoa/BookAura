package com.bookaura.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuthLoginFailureHandler implements AuthenticationFailureHandler {

    private final String frontendUrl;

    public OAuthLoginFailureHandler(@Value("${bookaura.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        String target = UriComponentsBuilder.fromUriString(frontendUrl)
                .pathSegment("oauth", "callback")
                .queryParam("error", "oauth_failed")
                .build().encode().toUriString();
        response.sendRedirect(target);
    }
}
