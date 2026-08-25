package com.bookaura.auth.sms;

import com.bookaura.common.error.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrevoSmsSenderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<HttpExchangeSnapshot> request = new AtomicReference<>();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsBrevoRequestWithConfiguredHeaderAndNormalizedE164Recipient() throws Exception {
        startServer(201, "{\"reference\":\"provider-reference\"}");
        BrevoSmsSender sender = sender("test-only-placeholder", "BookAura");
        String code = testCode();

        sender.sendOtp("+84 912-345-678", code);

        HttpExchangeSnapshot captured = request.get();
        assertThat(captured.method()).isEqualTo("POST");
        assertThat(captured.path()).isEqualTo("/v3/transactionalSMS/sms");
        assertThat(captured.apiKey()).isEqualTo("test-only-placeholder");
        assertThat(captured.contentType()).contains("application/json");
        Map<String, String> payload = objectMapper.readValue(captured.body(), new TypeReference<>() {
        });
        assertThat(payload).containsEntry("sender", "BookAura")
                .containsEntry("recipient", "+84912345678")
                .containsEntry("content", "Your BookAura verification code is " + code)
                .containsEntry("type", "transactional");
    }

    @Test
    void mapsProviderFailureToSafeApplicationError() {
        startServer(400, "{\"message\":\"provider detail\"}");
        BrevoSmsSender sender = sender("test-only-placeholder", "BookAura");
        String code = testCode();

        assertThatThrownBy(() -> sender.sendOtp("+84912345678", code))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getMessage()).isEqualTo("SMS delivery is unavailable");
                    assertThat(exception.code().name()).isEqualTo("SMS_DELIVERY_UNAVAILABLE");
                    assertThat(exception.getMessage()).doesNotContain(code, "test-only-placeholder");
                });
    }

    @Test
    void mapsNetworkFailureToSafeApplicationError() {
        startServer(201, "{}");
        BrevoSmsSender sender = sender("test-only-placeholder", "BookAura");
        String code = testCode();
        server.stop(0);

        assertThatThrownBy(() -> sender.sendOtp("+84912345678", code))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.code().name()).isEqualTo("SMS_DELIVERY_UNAVAILABLE");
                    assertThat(exception.getMessage()).doesNotContain(code, "test-only-placeholder");
                });
    }

    private BrevoSmsSender sender(String apiKey, String senderName) {
        SmsProperties properties = new SmsProperties();
        properties.setProvider("brevo");
        properties.getBrevo().setApiKey(apiKey);
        properties.getBrevo().setSender(senderName);
        properties.getBrevo().setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/v3");
        return new BrevoSmsSender(properties);
    }

    private void startServer(int status, String responseBody) {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/v3/transactionalSMS/sms", exchange -> {
                byte[] body = exchange.getRequestBody().readAllBytes();
                request.set(new HttpExchangeSnapshot(
                        exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                        exchange.getRequestHeaders().getFirst("api-key"),
                        exchange.getRequestHeaders().getFirst("content-type"),
                        new String(body, StandardCharsets.UTF_8)));
                byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
                try (exchange) {
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status, response.length);
                    exchange.getResponseBody().write(response);
                }
            });
            server.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start test HTTP server", exception);
        }
    }

    private static String testCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    private record HttpExchangeSnapshot(String method, String path, String apiKey,
                                        String contentType, String body) {
    }
}
