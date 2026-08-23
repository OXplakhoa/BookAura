package com.bookaura.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Metadata + bounded/redacted JSON request/response logging.
 * Never logs headers/query strings. Auth/OAuth/infra/multipart bodies are not wrapped or cached.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class HttpExchangeLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpExchangeLoggingFilter.class);
    private static final int REQUEST_CACHE_LIMIT = 4_096;

    private final SafePayloadSanitizer sanitizer;

    public HttpExchangeLoggingFilter(SafePayloadSanitizer sanitizer) {
        this.sanitizer = sanitizer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        if (!captureBodies(request)) {
            try {
                chain.doFilter(request, response);
            } finally {
                logExchange(request.getMethod(), request.getRequestURI(), response.getStatus(), started,
                        "<suppressed>", "<suppressed>");
            }
            return;
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            String requestBody = isJson(requestWrapper.getContentType())
                    ? sanitizer.sanitize(requestWrapper.getContentAsByteArray()) : "<not-json>";
            String responseBody = isJson(responseWrapper.getContentType())
                    ? sanitizer.sanitize(responseWrapper.getContentAsByteArray()) : "<not-json>";
            logExchange(request.getMethod(), request.getRequestURI(), responseWrapper.getStatus(), started,
                    requestBody, responseBody);
            responseWrapper.copyBodyToResponse();
        }
    }

    private boolean captureBodies(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contentType = request.getContentType();
        return !path.startsWith("/api/auth")
                && !path.startsWith("/oauth2")
                && !path.startsWith("/login/oauth2")
                && !path.startsWith("/v3/api-docs")
                && !path.startsWith("/swagger-ui")
                && !path.startsWith("/actuator")
                && (contentType == null || !contentType.toLowerCase().startsWith("multipart/"));
    }

    private boolean isJson(String contentType) {
        return contentType != null && contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private void logExchange(String method, String path, int status, long started,
                             String requestBody, String responseBody) {
        long durationMs = (System.nanoTime() - started) / 1_000_000;
        log.info("http_exchange method={} path={} status={} durationMs={} requestBody={} responseBody={}",
                method, path, status, durationMs, requestBody, responseBody);
    }
}
