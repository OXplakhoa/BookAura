package com.bookaura.recommendation;

import java.util.List;
import java.util.Set;

/**
 * One aura reading request. At least one mood or theme is required (validated in controller).
 *
 * @param moods       subset of the 7 fixed moods (may be empty if themes given)
 * @param timeMinutes optional reading-time budget in minutes (null = any length)
 * @param themes      free-form category/tag names the user picked (may be empty if moods given)
 * @param intensity   optional depth preference (null = any depth)
 */
public record AuraQuery(Set<Mood> moods, Integer timeMinutes, Set<String> themes, Intensity intensity) {

    public AuraQuery {
        moods = moods == null ? Set.of() : Set.copyOf(moods);
        themes = themes == null ? Set.of() : Set.copyOf(themes);
    }

    public boolean hasSignals() {
        return !moods.isEmpty() || !themes.isEmpty();
    }
}
