package com.bookaura.recommendation;

import java.util.List;
import java.util.UUID;

/**
 * One scored aura hit. {@code reasons} are human-readable rule explanations (why this book),
 * {@code matchedTags} are the concrete book tags that fired — both required by the P2 spec.
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
        List<String> reasons,
        List<String> matchedTags
) {
}
