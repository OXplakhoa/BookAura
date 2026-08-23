package com.bookaura.common.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Parses JSON, recursively redacts sensitive fields, then applies a strict log length cap. */
@Component
public class SafePayloadSanitizer {

    public static final int MAX_LOG_CHARS = 2_000;
    private static final String REDACTED = "[REDACTED]";
    private static final Set<String> SENSITIVE_EXACT = Set.of(
            "authorization", "otp", "code", "secret", "clientsecret", "refreshtoken", "accesstoken", "token",
            "email", "phone", "address", "dateofbirth", "fullname", "membername");

    private final ObjectMapper objectMapper;

    public SafePayloadSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String sanitize(byte[] body) {
        if (body == null || body.length == 0) return "<empty>";
        try {
            JsonNode root = objectMapper.readTree(body);
            redact(root);
            return truncate(objectMapper.writeValueAsString(root));
        } catch (Exception ex) {
            // Never fall back to raw text: malformed payloads may still contain secrets.
            return "<unparseable-json>";
        }
    }

    private void redact(JsonNode node) {
        if (node instanceof ObjectNode object) {
            List<String> fields = new ArrayList<>();
            object.fieldNames().forEachRemaining(fields::add);
            for (String field : fields) {
                if (isSensitive(field)) object.put(field, REDACTED);
                else redact(object.get(field));
            }
        } else if (node instanceof ArrayNode array) {
            array.forEach(this::redact);
        }
    }

    private boolean isSensitive(String field) {
        String normalized = field.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return normalized.contains("password") || SENSITIVE_EXACT.contains(normalized)
                || normalized.endsWith("secret") || normalized.endsWith("token")
                || normalized.endsWith("code");
    }

    private String truncate(String value) {
        return value.length() <= MAX_LOG_CHARS
                ? value
                : value.substring(0, MAX_LOG_CHARS) + "...[TRUNCATED]";
    }
}
