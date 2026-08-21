package com.bookaura.common.error;

/**
 * Domain/business failure carrying a stable {@link ErrorCode}.
 * Mapped to the consistent ApiError contract by GlobalExceptionHandler.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
