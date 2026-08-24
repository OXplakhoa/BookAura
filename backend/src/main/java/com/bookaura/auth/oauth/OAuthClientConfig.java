package com.bookaura.auth.oauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers providers only when both credentials are non-blank. Local/test boots stay usable without secrets.
 * Google is true OIDC; Facebook is classic OAuth2 + Graph API /me (no id_token) — see D29.
 */
@Configuration
public class OAuthClientConfig {

    /** Pinned Graph version for Facebook URLs; Meta deprecates older versions on a schedule. */
    private static final String FACEBOOK_GRAPH_VERSION = "v21.0";

    @Bean
    @Conditional(AnyCredentialsPresent.class)
    ClientRegistrationRepository oauthClientRegistrationRepository(
            org.springframework.core.env.Environment environment) {
        List<ClientRegistration> registrations = new ArrayList<>();
        if (credentialsPresent(environment, "google")) {
            registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(environment.getRequiredProperty("bookaura.oauth.google.client-id"))
                    .clientSecret(environment.getRequiredProperty("bookaura.oauth.google.client-secret"))
                    .scope("openid", "profile", "email")
                    .build());
        }
        if (credentialsPresent(environment, "facebook")) {
            registrations.add(facebookRegistration(environment));
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    /**
     * Facebook Login = standard OAuth2 authorization-code flow, but identity comes from Graph API
     * {@code /me?fields=id,name,email} instead of an OIDC id_token. Email may be absent when the
     * user denies the email permission or signed up with a phone number.
     */
    private static ClientRegistration facebookRegistration(org.springframework.core.env.Environment environment) {
        String graph = "https://graph.facebook.com/" + FACEBOOK_GRAPH_VERSION;
        return ClientRegistration.withRegistrationId("facebook")
                .clientId(environment.getRequiredProperty("bookaura.oauth.facebook.client-id"))
                .clientSecret(environment.getRequiredProperty("bookaura.oauth.facebook.client-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email", "public_profile")
                .authorizationUri("https://www.facebook.com/" + FACEBOOK_GRAPH_VERSION + "/dialog/oauth")
                .tokenUri(graph + "/oauth/access_token")
                .userInfoUri(graph + "/me?fields=id,name,email")
                .userNameAttributeName("id")
                .clientName("Facebook")
                .build();
    }

    private static boolean credentialsPresent(org.springframework.core.env.Environment environment, String provider) {
        String clientId = environment.getProperty("bookaura.oauth." + provider + ".client-id");
        String clientSecret = environment.getProperty("bookaura.oauth." + provider + ".client-secret");
        return org.springframework.util.StringUtils.hasText(clientId)
                && org.springframework.util.StringUtils.hasText(clientSecret);
    }

    static class AnyCredentialsPresent implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return credentialsPresent(context.getEnvironment(), "google")
                    || credentialsPresent(context.getEnvironment(), "facebook");
        }
    }
}
