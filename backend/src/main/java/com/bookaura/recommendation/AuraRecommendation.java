package com.bookaura.recommendation;

import java.util.List;
import java.util.UUID;

/**
 * One scored aura hit. {@code reasons} are human-readable rule explanations (why this book),
 * {@code breakdown} exposes exact signal contributions, and {@code matchedTags} are the concrete
 * book tags that fired — all required for an auditable P2 recommendation.
 */
public record AuraRecommendation(
        UUID bookId,
        String title,
        List<String> authors,
        List<String> categories,
        Integer publicationYear,
        Integer pageCount,
        int availableQuantity,
        int score,
        AuraScoreBreakdown breakdown,
        List<String> reasons,
        List<String> matchedTags
) {
}
