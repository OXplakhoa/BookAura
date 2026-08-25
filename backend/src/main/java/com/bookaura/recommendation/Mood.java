package com.bookaura.recommendation;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Fixed aura vocabulary (frozen demo scope). Each mood declares deterministic affinities:
 * tag hits are strong signals (+3), category hits are weaker (+2). See D30.
 */
public enum Mood {

    COZY(Set.of("cozy", "heartwarming", "comfort", "gentle", "slice-of-life", "feel-good"),
            Set.of("romance", "poetry", "fiction", "children")),
    ADVENTUROUS(Set.of("adventure", "quest", "epic", "action", "journey", "exploration"),
            Set.of("adventure", "fantasy", "sci-fi", "science fiction", "history")),
    ROMANTIC(Set.of("romance", "love", "slow-burn", "relationship", "passion"),
            Set.of("romance", "poetry", "drama")),
    DARK(Set.of("dark", "dystopia", "tragedy", "horror", "gothic", "noir"),
            Set.of("horror", "thriller", "dystopia", "crime", "mystery")),
    FUNNY(Set.of("humor", "satire", "witty", "comedy", "absurd"),
            Set.of("humor", "comedy", "fiction", "satire")),
    THOUGHTFUL(Set.of("thoughtful", "philosophical", "reflective", "psychology", "essays", "meditative"),
            Set.of("philosophy", "psychology", "essays", "memoir", "self-help")),
    INSPIRING(Set.of("inspiring", "biography", "self-improvement", "uplifting", "triumph"),
            Set.of("biography", "self-help", "business", "history", "memoir"));

    private final Set<String> tagAffinities;
    private final Set<String> categoryAffinities;

    Mood(Set<String> tagAffinities, Set<String> categoryAffinities) {
        this.tagAffinities = tagAffinities;
        this.categoryAffinities = categoryAffinities;
    }

    public Set<String> tagAffinities() {
        return tagAffinities;
    }

    public Set<String> categoryAffinities() {
        return categoryAffinities;
    }

    /** Case-insensitive query-param parsing; empty Optional = unknown mood. */
    public static Optional<Mood> fromParam(String value) {
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }
}
