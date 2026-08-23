package com.bookaura.catalog.validation;

import java.util.Locale;

public final class IsbnUtils {

    private IsbnUtils() {
    }

    public static String normalize(String raw) {
        return raw == null ? null : raw.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String raw) {
        String isbn = normalize(raw);
        return isbn != null && (isValidIsbn10(isbn) || isValidIsbn13(isbn));
    }

    private static boolean isValidIsbn10(String isbn) {
        if (!isbn.matches("\\d{9}[\\dX]")) return false;
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            int digit = isbn.charAt(i) == 'X' ? 10 : isbn.charAt(i) - '0';
            sum += digit * (10 - i);
        }
        return sum % 11 == 0;
    }

    private static boolean isValidIsbn13(String isbn) {
        if (!isbn.matches("\\d{13}")) return false;
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = isbn.charAt(i) - '0';
            sum += digit * (i % 2 == 0 ? 1 : 3);
        }
        int check = (10 - (sum % 10)) % 10;
        return check == isbn.charAt(12) - '0';
    }
}
