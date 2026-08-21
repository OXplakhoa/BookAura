package com.bookaura.common.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class TokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenGenerator() {
    }

    /** 256-bit URL-safe opaque token (verification links, refresh tokens, one-time auth codes). */
    public static String urlSafeToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 6-digit numeric OTP (phone/email codes). Zero-padded. */
    public static String sixDigitCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
