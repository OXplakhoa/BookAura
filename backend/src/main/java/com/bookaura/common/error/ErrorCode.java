package com.bookaura.common.error;

import org.springframework.http.HttpStatus;

/**
 * Stable machine-readable error codes. Extend per feature slice.
 */
public enum ErrorCode {
    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN),
    TOKEN_REVOKED(HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
    REFRESH_MISSING(HttpStatus.UNAUTHORIZED),
    REFRESH_INVALID(HttpStatus.UNAUTHORIZED),
    REFRESH_REUSED(HttpStatus.UNAUTHORIZED),
    REFRESH_EXPIRED(HttpStatus.UNAUTHORIZED),

    // Verification / OTP
    VERIFICATION_TOKEN_INVALID(HttpStatus.BAD_REQUEST),
    VERIFICATION_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST),
    RESEND_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS),

    // Conflicts / resources
    DUPLICATE_EMAIL(HttpStatus.CONFLICT),
    DUPLICATE_PHONE(HttpStatus.CONFLICT),
    NOT_FOUND(HttpStatus.NOT_FOUND),

    // Generic business rule violation (feature slices add specific codes)
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
