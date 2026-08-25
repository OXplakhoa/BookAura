package com.bookaura.loan;

import com.bookaura.AbstractIntegrationTest;
import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.RoleRepository;
import com.bookaura.auth.repository.UserAccountRepository;
import com.bookaura.auth.token.JwtService;
import com.bookaura.catalog.dto.BookRequest;
import com.bookaura.catalog.dto.BookResponse;
import com.bookaura.catalog.repository.BookRepository;
import com.bookaura.catalog.service.BookService;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.loan.dto.LoanResponse;
import com.bookaura.loan.repository.LoanRepository;
import com.bookaura.loan.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(LoanFlowIntegrationTest.FailureHookConfig.class)
class LoanFlowIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicInteger ISBN_SEQUENCE = new AtomicInteger(300_000_000);

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private BookService bookService;
    @Autowired private BookRepository bookRepository;
    @Autowired private LoanService loanService;
    @Autowired private LoanRepository loanRepository;
    @Autowired private ArmableLoanFailureHook failureHook;

    private UserAccount user;
    private String userToken;
    private UserAccount admin;
    private String adminToken;

    @BeforeEach
    void setUpActors() {
        user = createAccount(Role.USER);
        userToken = jwtService.createAccessToken(user).token();
        admin = createAccount(Role.ADMIN);
        adminToken = jwtService.createAccessToken(admin).token();
    }

    @Test
    void borrowReturnActiveAndHistory_workEndToEnd() throws Exception {
        BookResponse book = createBook(2);

        MvcResult borrow = mockMvc.perform(post("/api/loans")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":\"" + book.id() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        UUID loanId = UUID.fromString(objectMapper.readTree(borrow.getResponse().getContentAsString()).get("id").asText());
        assertThat(bookRepository.findById(book.id()).orElseThrow().getAvailableQuantity()).isEqualTo(1);

        mockMvc.perform(get("/api/loans/active").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(loanId.toString()));

        mockMvc.perform(post("/api/loans/{id}/return", loanId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
        assertThat(bookRepository.findById(book.id()).orElseThrow().getAvailableQuantity()).isEqualTo(2);

        mockMvc.perform(get("/api/loans/active").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/loans/history").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(post("/api/loans/{id}/return", loanId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RETURN"));
    }

    @Test
    void outOfStockAndDuplicateActiveLoan_areRejectedWithoutInventoryDrift() throws Exception {
        BookResponse oneCopy = createBook(1);
        loanService.borrow(user.getId(), oneCopy.id());
        UserAccount secondUser = createAccount(Role.USER);

        assertThatThrownBy(() -> loanService.borrow(secondUser.getId(), oneCopy.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.BOOK_OUT_OF_STOCK);
        assertThat(bookRepository.findById(oneCopy.id()).orElseThrow().getAvailableQuantity()).isZero();

        BookResponse twoCopies = createBook(2);
        loanService.borrow(user.getId(), twoCopies.id());
        assertThatThrownBy(() -> loanService.borrow(user.getId(), twoCopies.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.DUPLICATE_ACTIVE_LOAN);
        assertThat(bookRepository.findById(twoCopies.id()).orElseThrow().getAvailableQuantity()).isEqualTo(1);
    }

    @Test
    void userCannotReturnAnotherMembersLoan_butAdminCan() throws Exception {
        BookResponse book = createBook(1);
        LoanResponse loan = loanService.borrow(user.getId(), book.id());
        UserAccount other = createAccount(Role.USER);
        String otherToken = jwtService.createAccessToken(other).token();

        mockMvc.perform(post("/api/loans/{id}/return", loan.id())
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LOAN_NOT_OWNED"));

        mockMvc.perform(post("/api/admin/loans/{id}/return", loan.id())
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/loans/{id}/return", loan.id())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
        assertThat(bookRepository.findById(book.id()).orElseThrow().getAvailableQuantity()).isEqualTo(1);
    }

    @Test
    void forcedBorrowFailure_rollsBackAtomicInventoryMutation() {
        BookResponse book = createBook(1);
        long loansBefore = loanRepository.count();
        failureHook.failNext(LoanMutation.BORROW);

        assertThatThrownBy(() -> loanService.borrow(user.getId(), book.id()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("test-only");

        assertThat(bookRepository.findById(book.id()).orElseThrow().getAvailableQuantity()).isEqualTo(1);
        assertThat(loanRepository.count()).isEqualTo(loansBefore);
    }

    @Test
    void forcedReturnFailure_rollsBackLoanStateAndInventory() {
        BookResponse book = createBook(1);
        LoanResponse loan = loanService.borrow(user.getId(), book.id());
        failureHook.failNext(LoanMutation.RETURN);

        assertThatThrownBy(() -> loanService.returnOwn(user.getId(), loan.id()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("test-only");

        assertThat(loanRepository.findById(loan.id()).orElseThrow().getReturnedAt()).isNull();
        assertThat(bookRepository.findById(book.id()).orElseThrow().getAvailableQuantity()).isZero();
    }

    @Test
    void twoUsersCompetingForFinalCopy_exactlyOneSucceeds() throws Exception {
        BookResponse book = createBook(1);
        UserAccount userA = createAccount(Role.USER);
        UserAccount userB = createAccount(Role.USER);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Outcome> first = executor.submit(() -> borrowConcurrently(userA.getId(), book.id(), ready, start));
            Future<Outcome> second = executor.submit(() -> borrowConcurrently(userB.getId(), book.id(), ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Outcome> outcomes = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertThat(outcomes).filteredOn(Outcome::success).hasSize(1);
            assertThat(outcomes).filteredOn(outcome -> !outcome.success())
                    .extracting(Outcome::errorCode).containsExactly(ErrorCode.BOOK_OUT_OF_STOCK);
            assertThat(bookRepository.findById(book.id()).orElseThrow().getAvailableQuantity()).isZero();
            assertThat(loanRepository.countByBook_IdAndReturnedAtIsNull(book.id())).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Outcome borrowConcurrently(UUID userId, UUID bookId, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
            loanService.borrow(userId, bookId);
            return new Outcome(true, null);
        } catch (BusinessException ex) {
            return new Outcome(false, ex.code());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private BookResponse createBook(int quantity) {
        String isbn = nextIsbn();
        return bookService.create(new BookRequest(
                "Loan Test " + isbn,
                isbn,
                "Loan integration test",
                2024,
                quantity,
                List.of("Loan Author " + isbn),
                List.of("Loan Category"),
                null, null,
                true));
    }

    private UserAccount createAccount(String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        UserAccount account = new UserAccount();
        account.setEmail(roleName.toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + "@loan.test");
        account.setPasswordHash(passwordEncoder.encode("Password1"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setEmailVerifiedAt(Instant.now());
        account.getRoles().add(role);
        MemberProfile profile = new MemberProfile();
        profile.setUserAccount(account);
        profile.setFullName("Loan Test " + roleName);
        account.setProfile(profile);
        return userRepository.save(account);
    }

    private String nextIsbn() {
        String first12 = "978" + String.format("%09d", ISBN_SEQUENCE.getAndIncrement());
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (first12.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return first12 + ((10 - sum % 10) % 10);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Outcome(boolean success, ErrorCode errorCode) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailureHookConfig {
        @Bean
        ArmableLoanFailureHook loanFailureHook() {
            return new ArmableLoanFailureHook();
        }
    }

    static class ArmableLoanFailureHook implements LoanFailureHook {
        private final AtomicReference<LoanMutation> nextFailure = new AtomicReference<>();

        void failNext(LoanMutation mutation) {
            nextFailure.set(mutation);
        }

        @Override
        public void afterMutation(LoanMutation mutation) {
            if (nextFailure.compareAndSet(mutation, null)) {
                throw new IllegalStateException("test-only forced " + mutation + " failure after mutation");
            }
        }
    }
}
