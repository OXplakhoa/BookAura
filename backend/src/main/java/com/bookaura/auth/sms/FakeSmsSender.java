package com.bookaura.auth.sms;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Local/test-only in-memory SMS outbox. Raw OTP values are deliberately never logged.
 */
@Component
@Profile({"local", "test"})
public class FakeSmsSender implements SmsSender {

    public record SentSms(String phone, String code, Instant sentAt) {
    }

    private final List<SentSms> sent = new CopyOnWriteArrayList<>();

    @Override
    public void sendOtp(String phone, String code) {
        sent.add(new SentSms(phone, code, Instant.now()));
    }

    public List<SentSms> sentMessages() {
        return List.copyOf(sent);
    }

    public SentSms lastTo(String phone) {
        return sentMessages().stream()
                .filter(message -> message.phone().equals(phone))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("No fake SMS captured for target"));
    }
}
