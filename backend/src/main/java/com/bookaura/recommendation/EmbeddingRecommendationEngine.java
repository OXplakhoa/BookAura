package com.bookaura.recommendation;

import com.bookaura.catalog.entity.Author;
import com.bookaura.catalog.entity.Book;
import com.bookaura.catalog.entity.Category;
import com.bookaura.catalog.repository.BookRepository;
import com.bookaura.common.logging.LogOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Experimental, offline semantic recommender (P2 #39).
 *
 * <p>This is deliberately not a call to an AI provider. It creates a small deterministic signed
 * feature vector from normalized text and metadata, then compares query/book vectors with cosine
 * similarity. The vocabulary bridge is intentionally visible in code: selected moods expand to
 * their declared affinities, themes expand through the same bounded aliases as the rule engine,
 * and page metadata becomes coarse time/pace tokens.</p>
 *
 * <p>The response keeps the existing Aura contract. The numeric score is a normalized semantic
 * affinity percentage, while the rule-specific breakdown is neutral because no rule points were
 * awarded. Reasons describe the semantic signal and only concrete book tags are returned as
 * {@code matchedTags}.</p>
 */
@Service
@LogOperation
@ConditionalOnProperty(name = "bookaura.recommendation.engine", havingValue = "embedding")
public class EmbeddingRecommendationEngine implements RecommendationEngine {

    static final int VECTOR_DIMENSIONS = 128;
    static final int MAX_RESULTS = 6;

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "by", "for", "from", "in", "of", "on", "or", "the", "to", "with",
            "your", "you");

    private final BookRepository bookRepository;

    public EmbeddingRecommendationEngine(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuraRecommendation> recommend(AuraQuery query, int limit) {
        int resultLimit = Math.min(Math.max(limit, 0), MAX_RESULTS);
        if (resultLimit == 0) {
            return List.of();
        }

        SemanticDocument queryDocument = queryDocument(query);
        List<AuraRecommendation> scored = new ArrayList<>();
        for (Book book : bookRepository.findByActiveTrue()) {
            if (!book.isActive()) {
                continue;
            }
            SemanticDocument bookDocument = bookDocument(book);
            int score = toPercentage(cosineSimilarity(queryDocument.vector(), bookDocument.vector()));
            if (score > 0) {
                scored.add(toRecommendation(book, score, queryDocument, bookDocument));
            }
        }

        scored.sort(Comparator.comparingInt(AuraRecommendation::score).reversed()
                .thenComparing(AuraRecommendation::title, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AuraRecommendation::bookId));
        return scored.stream().limit(resultLimit).toList();
    }

    private static SemanticDocument queryDocument(AuraQuery query) {
        TokenWeights weights = new TokenWeights();
        Set<String> explanationTokens = new TreeSet<>();

        query.moods().stream().sorted().forEach(mood -> {
            addText(weights, explanationTokens, mood.name(), 1.0);
            mood.tagAffinities().stream().sorted()
                    .forEach(affinity -> addText(weights, explanationTokens, affinity, 1.1));
            mood.categoryAffinities().stream().sorted()
                    .forEach(affinity -> addText(weights, explanationTokens, affinity, 0.9));
        });
        query.themes().stream().map(EmbeddingRecommendationEngine::normalizePhrase)
                .sorted().forEach(theme -> {
                    addText(weights, explanationTokens, theme, 1.5);
                    ThemeAliases.forTheme(theme).stream().sorted()
                            .forEach(alias -> addText(weights, explanationTokens, alias, 1.2));
                });

        if (query.intensity() != null) {
            weights.add(metadataToken("intensity", query.intensity().name()), 1.3);
        }
        if (query.timeMinutes() != null) {
            weights.add(metadataToken("time", timeBand(query.timeMinutes())), 1.2);
        }
        return new SemanticDocument(weights.vector(), explanationTokens);
    }

    private static SemanticDocument bookDocument(Book book) {
        TokenWeights weights = new TokenWeights();
        Set<String> plainTokens = new TreeSet<>();

        addText(weights, plainTokens, book.getTitle(), 1.0);
        book.getAuthors().stream().map(Author::getName).filter(Objects::nonNull)
                .forEach(author -> addText(weights, plainTokens, author, 0.8));
        book.getCategories().stream().map(Category::getName).filter(Objects::nonNull)
                .forEach(category -> addText(weights, plainTokens, category, 1.35));
        book.getTags().stream().filter(Objects::nonNull)
                .forEach(tag -> addText(weights, plainTokens, tag, 1.6));

        if (book.getPageCount() != null) {
            weights.add(metadataToken("intensity", intensityBand(book.getPageCount())), 1.1);
            weights.add(metadataToken("time", timeBand(book.getPageCount() * RuleBasedRecommendationEngine.MINUTES_PER_PAGE)), 1.1);
        }
        return new SemanticDocument(weights.vector(), plainTokens);
    }

    private static AuraRecommendation toRecommendation(Book book, int score,
                                                         SemanticDocument query,
                                                         SemanticDocument document) {
        Set<String> matchedSignals = new TreeSet<>(query.explanationTokens());
        matchedSignals.retainAll(document.explanationTokens());
        List<String> matchedTags = book.getTags().stream()
                .filter(Objects::nonNull)
                .filter(tag -> !intersection(tokenize(tag), query.explanationTokens()).isEmpty())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        List<String> reasons = new ArrayList<>();
        reasons.add("Semantic affinity: " + score + "% (local deterministic embedding)");
        if (!matchedSignals.isEmpty()) {
            reasons.add("Matched semantic signals: " + matchedSignals.stream().limit(6)
                    .collect(java.util.stream.Collectors.joining(", ")));
        }
        if (!matchedTags.isEmpty()) {
            reasons.add("Matched semantic tags: " + String.join(", ", matchedTags));
        }

        return new AuraRecommendation(
                book.getId(), book.getTitle(),
                book.getAuthors().stream().map(Author::getName)
                        .sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                book.getCategories().stream().map(Category::getName)
                        .sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                book.getPublicationYear(), book.getPageCount(), book.getAvailableQuantity(),
                score,
                // Embedding scores are not rule points; leave this explainability field neutral.
                new AuraScoreBreakdown(0, 0, 0, 0),
                List.copyOf(reasons), List.copyOf(matchedTags));
    }

    private static void addText(TokenWeights weights, Set<String> explanationTokens,
                                String text, double fieldWeight) {
        List<String> tokens = tokenize(text);
        tokens.forEach(token -> {
            weights.add(token, fieldWeight);
            explanationTokens.add(token);
        });
    }

    private static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT)).results()
                .map(match -> match.group())
                .filter(token -> token.length() > 1 && !STOP_WORDS.contains(token))
                .distinct()
                .toList();
    }

    private static String normalizePhrase(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static Set<String> intersection(Collection<String> left, Set<String> right) {
        Set<String> result = new LinkedHashSet<>(left);
        result.retainAll(right);
        return result;
    }

    private static String metadataToken(String kind, String value) {
        return "metadata:" + kind + ":" + value.toLowerCase(Locale.ROOT);
    }

    private static String intensityBand(int pageCount) {
        if (pageCount <= 250) {
            return "light";
        }
        if (pageCount <= 500) {
            return "medium";
        }
        return "deep";
    }

    private static String timeBand(double minutes) {
        if (minutes <= 120) {
            return "short";
        }
        if (minutes <= 360) {
            return "medium";
        }
        return "long";
    }

    private static int toPercentage(double cosine) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, cosine)) * 100.0);
    }

    private static double cosineSimilarity(double[] left, double[] right) {
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    /** Small deterministic signed-hash vector. No network or model files are involved. */
    private static final class TokenWeights {
        private final Map<String, Double> weights = new java.util.HashMap<>();

        void add(String token, double weight) {
            weights.merge(token, weight, Double::sum);
        }

        double[] vector() {
            double[] vector = new double[VECTOR_DIMENSIONS];
            for (Map.Entry<String, Double> entry : weights.entrySet()) {
                int hash = stableHash(entry.getKey());
                int bucket = Math.floorMod(hash, VECTOR_DIMENSIONS);
                int sign = (Integer.rotateLeft(hash, 13) & 1) == 0 ? 1 : -1;
                vector[bucket] += sign * entry.getValue();
            }
            return vector;
        }

        private static int stableHash(String token) {
            int hash = 0x811c9dc5;
            for (int i = 0; i < token.length(); i++) {
                hash ^= token.charAt(i);
                hash *= 0x01000193;
            }
            return hash;
        }
    }

    private record SemanticDocument(double[] vector, Set<String> explanationTokens) {
        private SemanticDocument {
            explanationTokens = Set.copyOf(explanationTokens);
        }
    }
}
