package com.bookaura.auth.sms;

import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.common.util.PhoneNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Objects;

/**
 * Small credential-conditional Brevo transactional SMS client.
 *
 * <p>The current Brevo endpoint is {@code POST /v3/transactionalSMS/sms}. The API key is sent in
 * the {@code api-key} header and the request body contains only the configured sender, normalized
 * recipient, OTP message and transactional type. Provider response bodies are deliberately never
 * read into logs or application errors.</p>
 */
public class BrevoSmsSender implements SmsSender {

    private static final Logger LOG = LoggerFactory.getLogger(BrevoSmsSender.class);
    private static final String SMS_PATH = "/transactionalSMS/sms";
    private static final String GENERIC_FAILURE = "SMS delivery is unavailable";

    private final RestClient client;
    private final String apiKey;
    private final String sender;

    public BrevoSmsSender(SmsProperties properties) {
        this(buildClient(properties), properties.getBrevo().getApiKey(), properties.getBrevo().getSender());
    }

    BrevoSmsSender(RestClient client, String apiKey, String sender) {
        this.client = Objects.requireNonNull(client, "client");
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.sender = sender == null ? "" : sender.trim();
    }

    @Override
    public void sendOtp(String phone, String code) {
        String recipient = PhoneNormalizer.normalize(phone);
        if (recipient == null || code == null || code.isBlank() || apiKey.isBlank() || sender.isBlank()) {
            throw unavailable();
        }

        BrevoSmsRequest payload = new BrevoSmsRequest(
                sender,
                recipient,
                "Your BookAura verification code is " + code,
                "transactional");
        try {
            client.post()
                    .uri(SMS_PATH)
                    .header("api-key", apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            // Status is safe operational metadata; never log exception/body because it may contain
            // provider details, and the body can echo request data.
            LOG.warn("Brevo SMS delivery failed status={}", exception.getStatusCode().value());
            throw unavailable();
        } catch (RestClientException exception) {
            // Covers connection failures, timeouts and response conversion failures without
            // exposing a URL, provider response, phone number or OTP to the caller.
            LOG.warn("Brevo SMS delivery failed due to a provider/network error");
            throw unavailable();
        }
    }

    private static RestClient buildClient(SmsProperties properties) {
        String baseUrl = properties.getBrevo().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.brevo.com/v3";
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(10_000);
        return RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl.trim()))
                .requestFactory(requestFactory)
                .build();
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static BusinessException unavailable() {
        return new BusinessException(ErrorCode.SMS_DELIVERY_UNAVAILABLE, GENERIC_FAILURE);
    }

    private record BrevoSmsRequest(String sender, String recipient, String content, String type) {
    }
}
