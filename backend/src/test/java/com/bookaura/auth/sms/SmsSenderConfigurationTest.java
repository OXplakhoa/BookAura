package com.bookaura.auth.sms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SmsSenderConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(SmsSenderConfiguration.class);

    @Test
    void localProfileAlwaysUsesFakeSender() {
        contextRunner.withPropertyValues("spring.profiles.active=local", "bookaura.sms.provider=brevo",
                        "bookaura.sms.brevo.api-key=would-not-be-used")
                .run(context -> {
                    assertThat(context).hasSingleBean(SmsSender.class);
                    assertThat(context.getBean(SmsSender.class)).isInstanceOf(FakeSmsSender.class);
                });
    }

    @Test
    void productionWithMissingCredentialsUsesUnavailableSender() {
        contextRunner.withPropertyValues("spring.profiles.active=prod", "bookaura.sms.provider=brevo",
                        "bookaura.sms.brevo.api-key=")
                .run(context -> {
                    assertThat(context).hasSingleBean(SmsSender.class);
                    assertThat(context.getBean(SmsSender.class)).isInstanceOf(UnavailableSmsSender.class);
                });
    }

    @Test
    void productionWithBrevoCredentialsUsesBrevoSender() {
        contextRunner.withPropertyValues("spring.profiles.active=prod", "bookaura.sms.provider=brevo",
                        "bookaura.sms.brevo.api-key=test-only-key")
                .run(context -> {
                    assertThat(context).hasSingleBean(SmsSender.class);
                    assertThat(context.getBean(SmsSender.class)).isInstanceOf(BrevoSmsSender.class);
                });
    }

    @Test
    void unsupportedProductionProviderDoesNotFallBackToFake() {
        contextRunner.withPropertyValues("spring.profiles.active=prod", "bookaura.sms.provider=twilio")
                .run(context -> {
                    assertThat(context).hasSingleBean(SmsSender.class);
                    assertThat(context.getBean(SmsSender.class)).isInstanceOf(UnavailableSmsSender.class);
                });
    }
}
