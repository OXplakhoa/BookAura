package com.bookaura.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Consistent API error contract returned for every failure.
 * Never contains stack traces or internal implementation details.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String error,
        String message,
        String path,
        String traceId,
        Map<String, String> validationErrors
) {
    public static ApiError of(int status, String code, String error, String message,
                              String path, String traceId) {
        return new ApiError(Instant.now(), status, code, error, message, path, traceId, null);
    }

    public static ApiError ofValidation(int status, String code, String error, String message,
                                        String path, String traceId, Map<String, String> validationErrors) {
        return new ApiError(Instant.now(), status, code, error, message, path, traceId, validationErrors);
    }
}
