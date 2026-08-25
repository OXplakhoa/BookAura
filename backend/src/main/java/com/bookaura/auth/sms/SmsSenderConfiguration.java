package com.bookaura.auth.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * Selects exactly one SMS sender. Local/test never consult provider credentials; non-local/test
 * uses Brevo only when both the provider name and API key are configured, otherwise it fails
 * explicitly instead of silently pretending that a message was delivered.
 */
@Configuration(proxyBeanMethods = false)
public class SmsSenderConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "bookaura.sms")
    public SmsProperties smsProperties() {
        return new SmsProperties();
    }

    @Bean
    @Profile({"local", "test"})
    public FakeSmsSender fakeSmsSender() {
        return new FakeSmsSender();
    }

    @Bean
    @Profile("!local & !test")
    @Conditional(BrevoConfiguredCondition.class)
    public BrevoSmsSender brevoSmsSender(SmsProperties properties) {
        return new BrevoSmsSender(properties);
    }

    @Bean
    @Profile("!local & !test")
    @Conditional(BrevoUnavailableCondition.class)
    public UnavailableSmsSender unavailableSmsSender() {
        return new UnavailableSmsSender();
    }

    public static class BrevoConfiguredCondition implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                               org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            return isBrevoConfigured(context.getEnvironment());
        }
    }

    public static class BrevoUnavailableCondition implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                               org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            return !isBrevoConfigured(context.getEnvironment());
        }
    }

    private static boolean isBrevoConfigured(Environment environment) {
        String provider = environment.getProperty("bookaura.sms.provider", "");
        String apiKey = environment.getProperty("bookaura.sms.brevo.api-key", "");
        return "brevo".equalsIgnoreCase(provider.trim()) && !apiKey.isBlank();
    }
}
