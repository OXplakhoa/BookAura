package com.bookaura.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SafePayloadSanitizerTest {

    private final SafePayloadSanitizer sanitizer = new SafePayloadSanitizer(new ObjectMapper());

    @Test
    void recursivelyRedactsSensitiveFieldsWithoutLosingSafeContext() {
        String json = """
                {"email":"reader@test.dev","newEmail":"new@test.dev","initialPassword":"secret1","nested":{
                "access_token":"jwt-value","verificationCode":"123456"},
                "items":[{"clientSecret":"oauth-secret","title":"Clean Code"}]}
                """;
        String sanitized = sanitizer.sanitize(json.getBytes(StandardCharsets.UTF_8));

        assertThat(sanitized).contains("Clean Code", "[REDACTED]");
        assertThat(sanitized).doesNotContain(
                "reader@test.dev", "new@test.dev", "secret1", "jwt-value", "123456", "oauth-secret");
    }

    @Test
    void malformedJsonNeverFallsBackToRawSecret() {
        String sanitized = sanitizer.sanitize("{password: raw-secret".getBytes(StandardCharsets.UTF_8));
        assertThat(sanitized).isEqualTo("<unparseable-json>").doesNotContain("raw-secret");
    }

    @Test
    void capsLargePayload() {
        String json = "{\"description\":\"" + "a".repeat(3_000) + "\"}";
        String sanitized = sanitizer.sanitize(json.getBytes(StandardCharsets.UTF_8));
        assertThat(sanitized).hasSize(SafePayloadSanitizer.MAX_LOG_CHARS + "...[TRUNCATED]".length());
        assertThat(sanitized).endsWith("...[TRUNCATED]");
    }
}
