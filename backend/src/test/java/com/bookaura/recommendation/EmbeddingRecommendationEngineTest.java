package com.bookaura.recommendation;

import com.bookaura.catalog.entity.Author;
import com.bookaura.catalog.entity.Book;
import com.bookaura.catalog.entity.Category;
import com.bookaura.catalog.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingRecommendationEngineTest {

    @Mock
    private BookRepository bookRepository;

    private EmbeddingRecommendationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new EmbeddingRecommendationEngine(bookRepository);
    }

    @Test
    void semanticMatchRanksAboveUnrelatedBook() {
        Book match = book(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "A Cozy Journey", 240, Set.of("cozy", "heartwarming"), Set.of("Fiction"));
        Book unrelated = book(UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "The Ledger Manual", 700, Set.of("accounting"), Set.of("Finance"));
        when(bookRepository.findByActiveTrue()).thenReturn(List.of(unrelated, match));

        List<AuraRecommendation> result = engine.recommend(
                new AuraQuery(Set.of(Mood.COZY), null, Set.of(), null), 6);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("A Cozy Journey");
        assertThat(result.get(0).score()).isGreaterThan(0);
    }

    @Test
    void sameInputHasDeterministicOrderingRegardlessOfRepositoryOrder() {
        Book first = book(UUID.fromString("00000000-0000-0000-0000-000000000011"),
                "Same Signal", 200, Set.of("cozy"), Set.of());
        Book second = book(UUID.fromString("00000000-0000-0000-0000-000000000012"),
                "Same Signal", 200, Set.of("cozy"), Set.of());
        AuraQuery query = new AuraQuery(Set.of(Mood.COZY), null, Set.of(), null);

        when(bookRepository.findByActiveTrue()).thenReturn(List.of(first, second));
        List<AuraRecommendation> one = engine.recommend(query, 6);
        when(bookRepository.findByActiveTrue()).thenReturn(List.of(second, first));
        List<AuraRecommendation> two = engine.recommend(query, 6);

        assertThat(one).isEqualTo(two);
        assertThat(one).extracting(AuraRecommendation::bookId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void capsAtSixAndUsesOnlyActiveRepositoryResults() {
        List<Book> active = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            active.add(book(UUID.nameUUIDFromBytes(("active-" + i).getBytes()),
                    "Cozy Volume " + i, 200, Set.of("cozy"), Set.of()));
        }
        Book inactive = book(UUID.fromString("00000000-0000-0000-0000-000000000099"),
                "Inactive Cozy Volume", 200, Set.of("cozy"), Set.of());
        inactive.setActive(false);
        active.add(inactive);
        when(bookRepository.findByActiveTrue()).thenReturn(active);

        List<AuraRecommendation> result = engine.recommend(
                new AuraQuery(Set.of(Mood.COZY), null, Set.of(), null), 99);

        assertThat(result).hasSize(6);
        assertThat(result).allMatch(recommendation -> recommendation.score() > 0);
        assertThat(result).extracting(AuraRecommendation::title).doesNotContain("Inactive Cozy Volume");
    }

    @Test
    void reasonsAndMatchedTagsDescribeSemanticSignalsWithoutRulePoints() {
        Book philosophy = book(UUID.fromString("00000000-0000-0000-0000-000000000021"),
                "Quiet Notes", 304, Set.of("philosophical"), Set.of("Philosophy"));
        when(bookRepository.findByActiveTrue()).thenReturn(List.of(philosophy));

        AuraRecommendation result = engine.recommend(
                new AuraQuery(Set.of(), null, Set.of("Philosophy"), null), 6).get(0);

        assertThat(result.score()).isBetween(1, 100);
        assertThat(result.breakdown()).isEqualTo(new AuraScoreBreakdown(0, 0, 0, 0));
        assertThat(result.reasons()).anyMatch(reason -> reason.startsWith("Semantic affinity:"));
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("Matched semantic signals"));
        assertThat(result.matchedTags()).containsExactly("philosophical");
        assertThat(result.reasons()).noneMatch(reason -> reason.contains("+4") || reason.contains("+3"));
    }

    private static Book book(UUID id, String title, Integer pages, Set<String> tags, Set<String> categories) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setIsbn("9780000000000");
        book.setPublicationYear(2024);
        book.setTotalQuantity(2);
        book.setAvailableQuantity(1);
        book.setActive(true);
        book.setPageCount(pages);
        book.setTags(new java.util.LinkedHashSet<>(tags));
        categories.forEach(name -> {
            Category category = new Category();
            category.setName(name);
            book.getCategories().add(category);
        });
        Author author = new Author();
        author.setName("Test Author");
        book.getAuthors().add(author);
        return book;
    }
}
