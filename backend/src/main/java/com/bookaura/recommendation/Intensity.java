package com.bookaura.recommendation;

import java.util.Locale;
import java.util.Optional;

/**
 * Reading-depth preference mapped to page-count bands (D30). Null page counts stay neutral.
 */
public enum Intensity {

    LIGHT(1, 250),
    MEDIUM(251, 500),
    DEEP(501, Integer.MAX_VALUE);

    private final int minPages;
    private final int maxPages;

    Intensity(int minPages, int maxPages) {
        this.minPages = minPages;
        this.maxPages = maxPages;
    }

    public boolean contains(int pageCount) {
        return pageCount >= minPages && pageCount <= maxPages;
    }

    /** Adjacent band = mild fit (+1); farther = neutral (0). */
    public boolean adjacentTo(int pageCount) {
        return switch (this) {
            case LIGHT -> MEDIUM.contains(pageCount);
            case MEDIUM -> LIGHT.contains(pageCount) || DEEP.contains(pageCount);
            case DEEP -> MEDIUM.contains(pageCount);
        };
    }

    public static Optional<Intensity> fromParam(String value) {
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }
}
