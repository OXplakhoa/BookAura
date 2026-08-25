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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleBasedRecommendationEngineTest {

    @Mock
    private BookRepository bookRepository;

    private RuleBasedRecommendationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RuleBasedRecommendationEngine(bookRepository);
    }

    @Test
    void moodTagHitsScoreThreeEach_andProduceReasonsAndMatchedTags() {
        Book cozy = book("Hygge Nights", 200, Set.of("cozy", "comfort"), Set.of("Poetry"));
        when(bookRepository.findByActiveTrue()).thenReturn(List.of(cozy));

        List<AuraRecommendation> result = engine.recommend(
                new AuraQuery(Set.of(Mood.COZY), null, Set.of(), null), 6);

        assertThat(result).hasSize(1);
        AuraRecommendation hit = result.get(0);
        // 2 tag hits ×3 + 1 category hit ×2 = 8
        assertThat(hit.score()).isEqualTo(8);
        assertThat(hit.matchedTags()).containsExactlyInAnyOrder("cozy", "comfort");
        assertThat(hit.reasons()).anyMatch(r -> r.contains("cozy mood"));
        assertThat(hit.reasons()).anyMatch(r -> r.contains("Poetry"));
    }

    @Test
    void thoughtfulMoodRecognizesTheLiteralThoughtfulTag() {
        Book reflective = book("Quiet Notes", 180, Set.of("thoughtful"), Set.of());
        when(bookRepository.findByActiveTrue()).thenReturn(List.of(reflective));

        List<AuraRecommendation> result = engine.recommend(
                new AuraQuery(Set.of(Mood.THOUGHTFUL), null, Set.of(), null), 6);

        assertThat(result).singleElement().satisfies(hit -> {
            assertThat(hit.score()).isEqualTo(3);
            assertThat(hit.matchedTags()).containsExactly("thoughtful");
        });
    }

    @Test
    void explicitThemeOutweighsMoodSignal() {
        Book scifi = book("Star Cartography", 400, Set.of(), Set.of("Science Fiction"));
        Book moodOnly = book("Gentle Tales", 200, Set.of("cozy"), Set.of());
        when(bookRepository.findByActiveTrue()).thenReturn(List.of(scifi, moodOnly));

        List<AuraRecommendation> result = engine.recommend(
                new AuraQuery(Set.of(Mood.COZY), null, Set.of("Science Fiction"), null), 6);

        assertThat(result).extracting(r -> r.title())
                .containsExactly("Star Cartography", "Gentle Tales");
        assertThat(result.get(0).score()).isEqualTo(4); // theme hit
        assertThat(result.get(1).score()).isEqualTo(3); // mood tag hit
    }

    @Test
    void timeBudgetRewardsWellFittingBooks_andPenalizesOverlongOnes() {
        // 120min budget → 80 pages × 1.5 = 120min exact fit; 400 pages × 1.5 = 600min way over
        Book fits = book("Evening Novella", 80, Set.of("cozy"), Set.of());
        Book overlong = book("Infinite Archive", 400, Set.of("cozy"), Set.of());
        when(bookRepository.findByActiveTrue()).thenReturn(List.of(fits, overlong));

        List<AuraRecommendation> result = engine.recommend(
                new AuraQuery(Set.of(Mood.COZY), 120, Set.of(), null), 6);

        AuraRecommendation first = result.get(0);
        assertThat(first.title()).isEqualTo("Evening Novella");
        assertThat(first.score()).isEqualTo(3 + RuleBasedRecommendationEngine.TIME_PERFECT_POINTS);
        assertThat(first.reasons()).anyMatch(r -> r.contains("fills your time"));
        assertThat(result).extracting(AuraRecommendation::title).doesNotContain("Infinite Archive");
    }

    @Test
    void unknownPageCountIsNeutral_neverFilteredOut() {
        Book unknown = book("Mystery Length", null, Set.of("cozy"), Set.of());
        when(bookRepository.findByActiveTrue()).thenReturn(List.of(unknown));

        List<AuraRecommendation> result = engine.recommend(
                new AuraQuery(Set.of(Mood.COZY), 60, Set.of(), Intensity.LIGHT), 6);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).score()).isEqualTo(3); // only the mood tag hit
    }

    @Test
    void intensityExactBandBeatsAdjacentBand() {
        Book light = book("Light Verse", 100, Set.of("humor"), Set.of());
        Book mid = book("Mid Essays", 300, Set.of("humor"), Set.of());
        when(bookRepository.findByActiveTrue()).thenReturn(List.of(light, mid));

        List<AuraRecommendation> result = engine.recommend(
                new AuraQuery(Set.of(Mood.FUNNY), null, Set.of(), Intensity.LIGHT), 6);

        assertThat(result.get(0).title()).isEqualTo("Light Verse");
        assertThat(result.get(0).score()).isEqualTo(3 + 2); // exact band
        assertThat(result.get(1).score()).isEqualTo(3 + 1); // adjacent band
    }

    @Test
    void zeroScoreBooksAreExcluded_andResultsAreDeterministic() {
        Book match = book("Zephyr", 100, Set.of("adventure"), Set.of());
        Book noMatch = book("Tax Code Handbook", 900, Set.of("accounting"), Set.of("Reference"));
        when(bookRepository.findByActiveTrue()).thenReturn(List.of(noMatch, match));

        AuraQuery query = new AuraQuery(Set.of(Mood.ADVENTUROUS), null, Set.of(), null);
        List<AuraRecommendation> first = engine.recommend(query, 6);
        List<AuraRecommendation> second = engine.recommend(query, 6);

        assertThat(first).extracting(AuraRecommendation::title).containsExactly("Zephyr");
        assertThat(first).isEqualTo(second); // identical input ⇒ identical output
    }

    @Test
    void resultListIsCappedAtRequestedLimit() {
        List<Book> many = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(i -> book("Cozy Book " + i, 100 + i, Set.of("cozy"), Set.of()))
                .toList();
        when(bookRepository.findByActiveTrue()).thenReturn(many);

        List<AuraRecommendation> result = engine.recommend(
                new AuraQuery(Set.of(Mood.COZY), null, Set.of(), null), 6);

        assertThat(result).hasSize(6);
        // equal scores → deterministic title tie-break
        assertThat(result).extracting(AuraRecommendation::title)
                .containsExactly("Cozy Book 1", "Cozy Book 10", "Cozy Book 2",
                        "Cozy Book 3", "Cozy Book 4", "Cozy Book 5");
    }

    private static Book book(String title, Integer pageCount, Set<String> tags, Set<String> categories) {
        Book book = new Book();
        book.setId(UUID.randomUUID());
        book.setTitle(title);
        book.setIsbn("9780000000000");
        book.setPublicationYear(2000);
        book.setTotalQuantity(2);
        book.setAvailableQuantity(1);
        book.setActive(true);
        book.setPageCount(pageCount);
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
