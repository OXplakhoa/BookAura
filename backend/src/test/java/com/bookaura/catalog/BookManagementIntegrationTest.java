package com.bookaura.catalog;

import com.bookaura.AbstractIntegrationTest;
import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.RoleRepository;
import com.bookaura.auth.repository.UserAccountRepository;
import com.bookaura.auth.token.JwtService;
import com.bookaura.catalog.importcsv.CsvImportFailureHook;
import com.bookaura.catalog.repository.AuthorRepository;
import com.bookaura.catalog.repository.BookRepository;
import com.bookaura.catalog.repository.CategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(BookManagementIntegrationTest.FailureHookConfig.class)
class BookManagementIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicInteger ISBN_SEQUENCE = new AtomicInteger(100_000_000);

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private BookRepository bookRepository;
    @Autowired private AuthorRepository authorRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ArmableFailureHook failureHook;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUpTokens() {
        adminToken = tokenFor(Role.ADMIN);
        userToken = tokenFor(Role.USER);
    }

    @Test
    void adminCrud_softDeleteAndRoleAuthorization_workEndToEnd() throws Exception {
        String isbn = nextIsbn();
        Map<String, Object> request = bookJson("Domain-Driven Design", isbn, 2003, 3,
                List.of("Eric Evans"), List.of("Software Design"));

        MvcResult created = mockMvc.perform(post("/api/admin/books")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.availableQuantity").value(3))
                .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

        // Public detail requires no token.
        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Domain-Driven Design"));

        // Frontend guards are irrelevant: backend rejects USER mutations authoritatively.
        mockMvc.perform(put("/api/admin/books/{id}", id)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        request.put("totalQuantity", 5);
        mockMvc.perform(put("/api/admin/books/{id}", id)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(5))
                .andExpect(jsonPath("$.availableQuantity").value(5));

        mockMvc.perform(delete("/api/admin/books/{id}", id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
        mockMvc.perform(get("/api/admin/books/{id}", id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void duplicateNormalizedIsbn_returns409() throws Exception {
        String isbn = nextIsbn();
        createBook("First", isbn, 2000, 1, "Author A", "Category A");
        String formatted = isbn.substring(0, 3) + "-" + isbn.substring(3, 6) + "-" + isbn.substring(6);
        Map<String, Object> duplicate = bookJson("Duplicate", formatted, 2001, 1,
                List.of("Author B"), List.of("Category B"));

        mockMvc.perform(post("/api/admin/books")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ISBN"));
    }

    @Test
    void specificationSearch_combinesFiltersAndValidatesPaginationAndSort() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        createBook("Clean Alpha " + suffix, nextIsbn(), 2008, 2,
                "Robert Martin " + suffix, "Programming " + suffix);
        createBook("Clean Beta " + suffix, nextIsbn(), 2008, 0,
                "Other Author " + suffix, "Programming " + suffix);
        createBook("Clean Gamma " + suffix, nextIsbn(), 2009, 2,
                "Robert Martin " + suffix, "Architecture " + suffix);

        mockMvc.perform(get("/api/books")
                        .param("title", "clean")
                        .param("author", "Robert Martin " + suffix)
                        .param("category", "Programming " + suffix)
                        .param("available", "true")
                        .param("publicationYear", "2008")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "publicationYear:desc,title:asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Clean Alpha " + suffix))
                .andExpect(jsonPath("$.sort[0]").value("publicationYear:desc"))
                .andExpect(jsonPath("$.sort[1]").value("title:asc"));

        mockMvc.perform(get("/api/books").param("size", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAGE_SIZE_EXCEEDED"));
        mockMvc.perform(get("/api/books").param("sort", "passwordHash:asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
    }

    @Test
    void validCsv_importsRowsAndDeduplicatesRelations() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String isbn1 = nextIsbn();
        String isbn2 = nextIsbn();
        String author = "Shared Author " + suffix;
        String category = "Shared Category " + suffix;
        String csv = header()
                + "CSV Book One," + isbn1 + "," + author + "," + category + ",2020,2,First\n"
                + "CSV Book Two," + isbn2 + "," + author + "," + category + ",2021,1,Second\n";

        mockMvc.perform(multipart("/api/admin/books/import")
                        .file(csvFile("books.csv", csv))
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(2));

        assertThat(bookRepository.findAllByIsbnIn(Set.of(isbn1, isbn2))).hasSize(2);
        assertThat(authorRepository.findAllByNormalizedNameIn(Set.of(author.toLowerCase(Locale.ROOT)))).hasSize(1);
        assertThat(categoryRepository.findAllByNormalizedNameIn(Set.of(category.toLowerCase(Locale.ROOT)))).hasSize(1);
    }

    @Test
    void duplicateIsbnWithinCsv_returnsRowErrorAndImportsNothing() throws Exception {
        String isbn = nextIsbn();
        long before = bookRepository.count();
        String csv = header()
                + "Good Row," + isbn + ",Author X,Category X,2020,1,Good\n"
                + "Duplicate Row," + isbn + ",Author Y,Category Y,2021,1,Bad\n";

        mockMvc.perform(multipart("/api/admin/books/import")
                        .file(csvFile("duplicate.csv", csv))
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CSV_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors['row[3].isbn']").value("Duplicate ISBN within this file"));
        assertThat(bookRepository.count()).isEqualTo(before);
    }

    @Test
    void testOnlyFailureAfterFlush_rollsBackBooksAuthorsAndCategories() throws Exception {
        long booksBefore = bookRepository.count();
        long authorsBefore = authorRepository.count();
        long categoriesBefore = categoryRepository.count();
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String csv = header() + "Rollback Book," + nextIsbn() + ",Rollback Author " + suffix
                + ",Rollback Category " + suffix + ",2022,1,Must roll back\n";
        failureHook.failNext();

        mockMvc.perform(multipart("/api/admin/books/import")
                        .file(csvFile("rollback.csv", csv))
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        assertThat(bookRepository.count()).isEqualTo(booksBefore);
        assertThat(authorRepository.count()).isEqualTo(authorsBefore);
        assertThat(categoryRepository.count()).isEqualTo(categoriesBefore);
    }

    @Test
    void csvFilePolicy_rejectsWrongExtensionAndFiveMiBFile() throws Exception {
        MockMultipartFile wrong = new MockMultipartFile("file", "books.txt", "text/plain", header().getBytes());
        mockMvc.perform(multipart("/api/admin/books/import").file(wrong)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE"));

        byte[] exactlyFiveMiB = new byte[5 * 1024 * 1024];
        Arrays.fill(exactlyFiveMiB, (byte) 'a');
        MockMultipartFile large = new MockMultipartFile("file", "large.csv", "text/csv", exactlyFiveMiB);
        mockMvc.perform(multipart("/api/admin/books/import").file(large)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    private UUID createBook(String title, String isbn, int year, int quantity, String author, String category)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/books")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bookJson(title, isbn, year, quantity, List.of(author), List.of(category)))))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private Map<String, Object> bookJson(String title, String isbn, int year, int quantity,
                                         List<String> authors, List<String> categories) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("isbn", isbn);
        body.put("description", "Test description");
        body.put("publicationYear", year);
        body.put("totalQuantity", quantity);
        body.put("authors", authors);
        body.put("categories", categories);
        body.put("active", true);
        return body;
    }

    private String tokenFor(String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        UserAccount user = new UserAccount();
        user.setEmail(roleName.toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + "@test.dev");
        user.setPasswordHash(passwordEncoder.encode("Password1"));
        user.setStatus(AccountStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        user.getRoles().add(role);
        MemberProfile profile = new MemberProfile();
        profile.setUserAccount(user);
        profile.setFullName("Test " + roleName);
        user.setProfile(profile);
        userRepository.save(user);
        return jwtService.createAccessToken(user).token();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String header() {
        return "title,isbn,authors,categories,publicationYear,totalQuantity,description\n";
    }

    private MockMultipartFile csvFile(String filename, String csv) {
        return new MockMultipartFile("file", filename, "text/csv", csv.getBytes(StandardCharsets.UTF_8));
    }

    /** Generates deterministic valid ISBN-13 values for tests. */
    private String nextIsbn() {
        String first12 = "978" + String.format("%09d", ISBN_SEQUENCE.getAndIncrement());
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (first12.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return first12 + ((10 - sum % 10) % 10);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailureHookConfig {
        @Bean
        ArmableFailureHook csvImportFailureHook() {
            return new ArmableFailureHook();
        }
    }

    static class ArmableFailureHook implements CsvImportFailureHook {
        private final AtomicBoolean armed = new AtomicBoolean();

        void failNext() {
            armed.set(true);
        }

        @Override
        public void afterPersist() {
            if (armed.getAndSet(false)) {
                throw new IllegalStateException("test-only forced failure after CSV flush");
            }
        }
    }
}
