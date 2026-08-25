package com.bookaura.recommendation;

import com.bookaura.AbstractIntegrationTest;
import com.bookaura.catalog.dto.BookRequest;
import com.bookaura.catalog.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuraIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicInteger ISBN_SEQUENCE = new AtomicInteger(700_000_000);

    @Autowired
    private BookService bookService;

    @Test
    void auraIsPublic_andReturnsScoredDeterministicResults() throws Exception {
        String tag = "aura-cozy-" + UUID.randomUUID().toString().substring(0, 8);
        createBook("Aura Hearth", 90, tag, "Poetry");
        createBook("Aura Tome", 900, tag, "Philosophy");

        mockMvc.perform(get("/api/recommendations/aura")
                        .param("moods", "cozy")
                        .param("themes", tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookId").isNotEmpty())
                .andExpect(jsonPath("$[0].score").isNumber())
                .andExpect(jsonPath("$[0].reasons").isArray())
                .andExpect(jsonPath("$[0].matchedTags").isArray())
                .andExpect(jsonPath("$[0].matchedTags[0]").value(tag));
    }

    @Test
    void auraRejectsMissingSignals_andUnknownMood_andBadTime() throws Exception {
        mockMvc.perform(get("/api/recommendations/aura"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AURA_INVALID_PARAM"));

        mockMvc.perform(get("/api/recommendations/aura").param("moods", "sparkly"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AURA_INVALID_PARAM"))
                .andExpect(jsonPath("$.message").value(containsString("cozy")));

        mockMvc.perform(get("/api/recommendations/aura")
                        .param("moods", "cozy").param("timeMinutes", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AURA_INVALID_PARAM"));
    }

    @Test
    void timeBudgetRanksShorterBookFirst() throws Exception {
        String tag = "aura-time-" + UUID.randomUUID().toString().substring(0, 8);
        createBook("Quick Lantern", 60, tag, "Fiction");
        createBook("Endless Atlas", 800, tag, "Fiction");

        mockMvc.perform(get("/api/recommendations/aura")
                        .param("themes", tag).param("timeMinutes", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Quick Lantern"))
                .andExpect(jsonPath("$[0].reasons[0]", containsString("In your theme")))
                .andExpect(jsonPath("$", hasSize(1))); // 800-page book is over budget → negative score → excluded
    }

    @Test
    void categoriesEndpointIsPublic_andListsNames() throws Exception {
        String category = "Aura Cat " + UUID.randomUUID().toString().substring(0, 8);
        createBook("Category Beacon", 120, null, category);

        mockMvc.perform(get("/api/books/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*]", org.hamcrest.Matchers.hasItem(category)));
    }

    private void createBook(String title, int pages, String tag, String category) {
        String isbn = "978" + String.format("%010d", ISBN_SEQUENCE.getAndIncrement());
        bookService.create(new BookRequest(
                title, isbn, "Aura integration test", 2020, 3,
                List.of("Aura Author"), List.of(category),
                pages, tag == null ? null : List.of(tag), true));
    }
}
