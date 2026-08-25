package com.bookaura.recommendation;

import java.util.List;
import java.util.UUID;

/**
 * One scored aura hit. {@code reasons} are human-readable engine explanations (why this book),
 * {@code breakdown} exposes exact rule contributions when the rule engine is active, and
 * {@code matchedTags} are the concrete book tags that fired — all required for an auditable P2
 * recommendation. The experimental semantic engine keeps the rule breakdown neutral.
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
