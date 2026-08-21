package com.bookaura.auth.email;

/**
 * Outbound email abstraction (D8). Implementations: SmtpEmailSender (Mailpit local / Brevo demo),
 * FakeEmailSender (tests). Never log message bodies containing secrets beyond the intended link.
 */
public interface EmailSender {

    void sendVerificationEmail(String to, String verificationLink);

    void send(String to, String subject, String body);
}
