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
    DUPLICATE_ISBN(HttpStatus.CONFLICT),
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND),
    INVENTORY_BELOW_BORROWED_COUNT(HttpStatus.CONFLICT),

    // Loans
    MEMBER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND),
    LOAN_NOT_FOUND(HttpStatus.NOT_FOUND),
    BOOK_OUT_OF_STOCK(HttpStatus.CONFLICT),
    DUPLICATE_ACTIVE_LOAN(HttpStatus.CONFLICT),
    DUPLICATE_RETURN(HttpStatus.CONFLICT),
    LOAN_NOT_OWNED(HttpStatus.FORBIDDEN),
    INVENTORY_INCONSISTENT(HttpStatus.CONFLICT),

    NOT_FOUND(HttpStatus.NOT_FOUND),

    // Pagination / sorting
    INVALID_PAGE(HttpStatus.BAD_REQUEST),
    PAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST),
    INVALID_SORT(HttpStatus.BAD_REQUEST),

    // CSV import
    CSV_VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    CSV_HEADER_INVALID(HttpStatus.BAD_REQUEST),
    UNSUPPORTED_FILE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),

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
