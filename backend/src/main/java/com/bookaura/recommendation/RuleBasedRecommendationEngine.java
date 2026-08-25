package com.bookaura.recommendation;

import com.bookaura.catalog.entity.Author;
import com.bookaura.catalog.entity.Book;
import com.bookaura.catalog.entity.Category;
import com.bookaura.catalog.repository.BookRepository;
import com.bookaura.common.logging.LogOperation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Deterministic Shelf Aura scorer (D30). Every point is traceable to a written rule:
 * <ul>
 *   <li>mood: +3 per matched book tag, +2 per matched category</li>
 *   <li>theme: +4 for a direct category match, +3 for a matching tag/known alias</li>
 *   <li>time: estimated minutes = pageCount × 1.5; fits budget well +4, quick fit +2,
 *       slightly over −2, way over −4; unknown pages = 0 (neutral)</li>
 *   <li>intensity: page band exact +2, adjacent +1; unknown pages = 0 (neutral)</li>
 * </ul>
 * Ordering: score desc, then title asc, then id — identical input ⇒ identical output.
 */
@Service
@LogOperation
public class RuleBasedRecommendationEngine implements RecommendationEngine {

    /** Casual sustained reading pace ≈ 40 pages/hour. */
    static final double MINUTES_PER_PAGE = 1.5;
    static final int MOOD_TAG_POINTS = 3;
    static final int MOOD_CATEGORY_POINTS = 2;
    static final int THEME_CATEGORY_POINTS = 4;
    static final int THEME_ALIAS_POINTS = 3;
    static final int TIME_PERFECT_POINTS = 4;
    static final int TIME_QUICK_POINTS = 2;
    static final int TIME_SLIGHTLY_OVER_PENALTY = -2;
    static final int TIME_WAY_OVER_PENALTY = -4;
    static final int INTENSITY_EXACT_POINTS = 2;
    static final int INTENSITY_ADJACENT_POINTS = 1;

    private final BookRepository bookRepository;

    public RuleBasedRecommendationEngine(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuraRecommendation> recommend(AuraQuery query, int limit) {
        Set<String> normalizedThemes = normalizeAll(query.themes());
        List<AuraRecommendation> scored = new ArrayList<>();
        for (Book book : bookRepository.findByActiveTrue()) {
            ScoreCard card = score(book, query, normalizedThemes);
            if (card.total() > 0) {
                scored.add(card.toRecommendation(book));
            }
        }
        scored.sort(Comparator.comparingInt(AuraRecommendation::score).reversed()
                .thenComparing(AuraRecommendation::title, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AuraRecommendation::bookId));
        return scored.stream().limit(limit).toList();
    }

    private ScoreCard score(Book book, AuraQuery query, Set<String> themes) {
        ScoreCard card = new ScoreCard();
        Set<String> bookTags = normalizeAll(book.getTags());
        Set<String> bookCategories = normalizeAll(
                book.getCategories().stream().map(Category::getName).toList());

        for (Mood mood : query.moods().stream().sorted().toList()) {
            Set<String> tagHits = intersect(bookTags, mood.tagAffinities());
            Set<String> categoryHits = intersect(bookCategories, mood.categoryAffinities());
            int tagPoints = tagHits.size() * MOOD_TAG_POINTS;
            int categoryPoints = categoryHits.size() * MOOD_CATEGORY_POINTS;
            card.moodPoints += tagPoints + categoryPoints;
            card.matchedTags.addAll(tagHits);
            tagHits.forEach(tag -> card.reasons.add(
                    "Matches your " + mood.name().toLowerCase(Locale.ROOT)
                            + " mood (tag: " + tag + ", +" + MOOD_TAG_POINTS + ")"));
            String moodLabel = mood.name().toLowerCase(Locale.ROOT);
            String article = startsWithVowel(moodLabel) ? "an" : "a";
            categoryHits.forEach(category -> card.reasons.add(
                    capitalize(category) + " fits " + article + " " + moodLabel
                            + " mood (+" + MOOD_CATEGORY_POINTS + ")"));
        }

        for (String theme : themes.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList()) {
            boolean categoryHit = bookCategories.contains(theme);
            Set<String> themeTagHits = intersect(bookTags, ThemeAliases.forTheme(theme));
            if (categoryHit) {
                card.themePoints += THEME_CATEGORY_POINTS;
                card.reasons.add("In your theme: " + capitalize(theme)
                        + " category (+" + THEME_CATEGORY_POINTS + ")");
            }
            if (!themeTagHits.isEmpty()) {
                card.themePoints += THEME_ALIAS_POINTS;
                card.matchedTags.addAll(themeTagHits);
                card.reasons.add("In your theme: " + capitalize(theme)
                        + " via tag " + String.join(", ", themeTagHits)
                        + " (+" + THEME_ALIAS_POINTS + ")");
            }
        }

        if (query.timeMinutes() != null && book.getPageCount() != null) {
            double estimatedMinutes = book.getPageCount() * MINUTES_PER_PAGE;
            double budget = query.timeMinutes();
            if (estimatedMinutes <= budget) {
                boolean fillsWell = estimatedMinutes >= budget * 0.6;
                card.timePoints += fillsWell ? TIME_PERFECT_POINTS : TIME_QUICK_POINTS;
                card.reasons.add("≈" + toHoursLabel(estimatedMinutes)
                        + (fillsWell ? " — fills your time nicely (+" + TIME_PERFECT_POINTS + ")"
                        : " — a quick fit for your slot (+" + TIME_QUICK_POINTS + ")"));
            } else {
                card.timePoints += estimatedMinutes <= budget * 1.5
                        ? TIME_SLIGHTLY_OVER_PENALTY : TIME_WAY_OVER_PENALTY;
                card.reasons.add("≈" + toHoursLabel(estimatedMinutes) + " exceeds your time budget ("
                        + card.timePoints + ")");
            }
        }

        if (query.intensity() != null && book.getPageCount() != null) {
            if (query.intensity().contains(book.getPageCount())) {
                card.intensityPoints += INTENSITY_EXACT_POINTS;
                card.reasons.add(query.intensity().name().toLowerCase(Locale.ROOT)
                        + " read — matches your pace (+" + INTENSITY_EXACT_POINTS + ")");
            } else if (query.intensity().adjacentTo(book.getPageCount())) {
                card.intensityPoints += INTENSITY_ADJACENT_POINTS;
                card.reasons.add("near your " + query.intensity().name().toLowerCase(Locale.ROOT)
                        + " pace (+" + INTENSITY_ADJACENT_POINTS + ")");
            }
        }
        return card;
    }

    private static Set<String> normalizeAll(java.util.Collection<String> values) {
        Set<String> normalized = new TreeSet<>();
        values.forEach(value -> normalized.add(value.trim().toLowerCase(Locale.ROOT)));
        return normalized;
    }

    private static Set<String> intersect(Set<String> left, Set<String> right) {
        Set<String> hits = new TreeSet<>(left);
        hits.retainAll(right);
        return hits;
    }

    private static String toHoursLabel(double minutes) {
        double hours = minutes / 60.0;
        return hours >= 1 ? String.format(Locale.ROOT, "%.1fh read", hours)
                : String.format(Locale.ROOT, "%.0fmin read", minutes);
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static boolean startsWithVowel(String value) {
        return !value.isEmpty() && "aeiou".indexOf(value.charAt(0)) >= 0;
    }

    /** Mutable accumulator while a book is being scored; reasons/tags keep insertion order. */
    private static final class ScoreCard {
        private int moodPoints;
        private int themePoints;
        private int timePoints;
        private int intensityPoints;
        private final List<String> reasons = new ArrayList<>();
        private final Set<String> matchedTags = new LinkedHashSet<>();

        int total() {
            return moodPoints + themePoints + timePoints + intensityPoints;
        }

        AuraRecommendation toRecommendation(Book book) {
            return new AuraRecommendation(
                    book.getId(), book.getTitle(),
                    book.getAuthors().stream().map(Author::getName)
                            .sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                    book.getCategories().stream().map(Category::getName)
                            .sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                    book.getPublicationYear(), book.getPageCount(), book.getAvailableQuantity(),
                    total(), new AuraScoreBreakdown(moodPoints, themePoints, timePoints, intensityPoints),
                    List.copyOf(reasons), List.copyOf(matchedTags));
        }
    }
}
