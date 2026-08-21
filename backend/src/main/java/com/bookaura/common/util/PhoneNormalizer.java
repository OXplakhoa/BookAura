package com.bookaura.common.util;

/**
 * Normalizes phone numbers before persistence/lookup: trims and removes spaces, dashes, dots.
 * Keeps a leading '+' (E.164 style). Returns null for blank input (phone is optional).
 */
public final class PhoneNormalizer {

    private PhoneNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().replaceAll("[\\s\\-.]", "");
        return normalized.isEmpty() ? null : normalized;
    }
}
