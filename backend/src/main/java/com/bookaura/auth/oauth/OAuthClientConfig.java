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

/**
 * Registers Google only when both credentials are non-blank. Local/test boots stay usable without secrets.
 */
@Configuration
public class OAuthClientConfig {

    @Bean
    @Conditional(GoogleCredentialsPresent.class)
    ClientRegistrationRepository googleClientRegistrationRepository(
            org.springframework.core.env.Environment environment) {
        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(environment.getRequiredProperty("bookaura.oauth.google.client-id"))
                .clientSecret(environment.getRequiredProperty("bookaura.oauth.google.client-secret"))
                .scope("openid", "profile", "email")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }

    static class GoogleCredentialsPresent implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String clientId = context.getEnvironment().getProperty("bookaura.oauth.google.client-id");
            String clientSecret = context.getEnvironment().getProperty("bookaura.oauth.google.client-secret");
            return org.springframework.util.StringUtils.hasText(clientId)
                    && org.springframework.util.StringUtils.hasText(clientSecret);
        }
    }
}
