package com.bookaura.auth.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TEST-ONLY sender: captures messages in memory so integration tests can extract
 * verification links/OTP codes without a mail server. Clearly labeled; never active outside tests.
 */
@Component
@Profile("test")
public class FakeEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(FakeEmailSender.class);

    public record SentMessage(String to, String subject, String body) {
    }

    private final List<SentMessage> sent = new CopyOnWriteArrayList<>();

    @Override
    public void sendVerificationEmail(String to, String verificationLink) {
        send(to, "Verify your BookAura account", "Verify: " + verificationLink);
    }

    @Override
    public void send(String to, String subject, String body) {
        sent.add(new SentMessage(to, subject, body));
        log.info("[FAKE-EMAIL test-only] to={} subject={}", to, subject);
    }

    public List<SentMessage> sentMessages() {
        return List.copyOf(sent);
    }

    public SentMessage lastTo(String to) {
        return sentMessages().stream()
                .filter(m -> m.to().equals(to))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("No email captured for " + to));
    }
}
