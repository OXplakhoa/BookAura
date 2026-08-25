package com.bookaura.member;

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
import com.bookaura.catalog.service.BookService;
import com.bookaura.loan.repository.LoanRepository;
import com.bookaura.loan.service.LoanService;
import com.bookaura.member.dto.MemberResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MemberManagementIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicInteger ISBN_SEQUENCE = new AtomicInteger(400_000_000);

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private BookService bookService;
    @Autowired private LoanService loanService;
    @Autowired private LoanRepository loanRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUpActors() {
        adminToken = tokenFor(createAccount(Role.ADMIN));
        userToken = tokenFor(createAccount(Role.USER));
    }

    @Test
    void adminCrudAndDisable_areAuthorizedValidatedAndKeepLoanHistory() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String email = "member-" + suffix + "@test.dev";
        String phone = "+8491" + String.format("%06d", Math.abs(suffix.hashCode()) % 1_000_000);
        Map<String, Object> createBody = createBody("Member " + suffix, email, phone,
                "1995-02-09", true, true);

        mockMvc.perform(post("/api/admin/members")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isForbidden());

        MemberResponse member = createMember(createBody);
        assertThat(member.email()).isEqualTo(email);
        assertThat(member.emailVerified()).isTrue();
        assertThat(member.roles()).containsExactly("USER");

        mockMvc.perform(post("/api/admin/members")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("fullName", "Updated " + suffix);
        update.put("phone", ""); // explicit clear
        update.put("dateOfBirth", "1994-03-10");
        update.put("address", "New address");
        update.put("active", true);
        mockMvc.perform(put("/api/admin/members/{id}", member.id())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated " + suffix))
                .andExpect(jsonPath("$.phone").doesNotExist());

        BookResponse book = createBook("Retention " + suffix, 1);
        loanService.borrow(member.userAccountId(), book.id());
        long loansBefore = loanRepository.count();
        mockMvc.perform(delete("/api/admin/members/{id}", member.id())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/members/{id}", member.id())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.email").value(email));
        assertThat(loanRepository.count()).isEqualTo(loansBefore);
    }

    @Test
    void search_combinesSevenConditionsIncludingBorrowedBookTitle() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String phone = "+8498" + String.format("%06d", Math.abs(suffix.hashCode()) % 1_000_000);
        MemberResponse target = createMember(createBody(
                "Nguyen Target " + suffix,
                "target-" + suffix + "@test.dev",
                phone,
                "1995-02-09",
                true,
                true));
        createMember(createBody(
                "Nguyen Other " + suffix,
                "other-" + suffix + "@test.dev",
                "+8477" + String.format("%06d", Math.abs((suffix + "x").hashCode()) % 1_000_000),
                "1980-01-01",
                false,
                true));
        BookResponse borrowed = createBook("Unique Borrowed " + suffix, 2);
        loanService.borrow(target.userAccountId(), borrowed.id());

        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", bearer(adminToken))
                        .param("name", "Target " + suffix)
                        .param("emailOrPhone", phone.substring(phone.length() - 5))
                        .param("dateOfBirthFrom", "1990/01/1")
                        .param("dateOfBirthTo", "2000/12/31")
                        .param("borrowedBookTitle", "Borrowed " + suffix)
                        .param("status", "ACTIVE")
                        .param("role", "USER")
                        .param("emailVerified", "true")
                        .param("sort", "email:asc,fullName:desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(target.id().toString()))
                .andExpect(jsonPath("$.sort[0]").value("userAccount.email:asc"))
                .andExpect(jsonPath("$.sort[1]").value("fullName:desc"));
    }

    @Test
    void invalidDateFormatAndRange_returnClear400() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", bearer(adminToken))
                        .param("dateOfBirthFrom", "2024-02-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("yyyy/MM/d")));

        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", bearer(adminToken))
                        .param("dateOfBirthFrom", "2024/02/30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));

        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", bearer(adminToken))
                        .param("dateOfBirthFrom", "2000/01/1")
                        .param("dateOfBirthTo", "1999/01/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    void searchRejectsPageAboveTenAndUnknownSort() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", bearer(adminToken)).param("size", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAGE_SIZE_EXCEEDED"));
        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", bearer(adminToken)).param("sort", "passwordHash:asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
    }

    @Test
    void updateToAnotherMembersPhone_returns409() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String phoneA = "+8451" + String.format("%06d", Math.abs(suffix.hashCode()) % 1_000_000);
        String phoneB = "+8452" + String.format("%06d", Math.abs((suffix + "b").hashCode()) % 1_000_000);
        MemberResponse first = createMember(createBody("First " + suffix,
                "first-" + suffix + "@test.dev", phoneA, "1990-01-01", true, true));
        createMember(createBody("Second " + suffix,
                "second-" + suffix + "@test.dev", phoneB, "1991-01-01", true, true));

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("fullName", "First " + suffix);
        update.put("phone", phoneB);
        update.put("dateOfBirth", "1990-01-01");
        update.put("address", "Address");
        update.put("active", true);
        mockMvc.perform(put("/api/admin/members/{id}", first.id())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PHONE"));
    }

    private MemberResponse createMember(Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/members")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), MemberResponse.class);
    }

    private Map<String, Object> createBody(String name, String email, String phone, String dob,
                                           boolean verified, boolean active) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fullName", name);
        body.put("email", email);
        body.put("phone", phone);
        body.put("initialPassword", "Password1");
        body.put("dateOfBirth", dob);
        body.put("address", "Test address");
        body.put("emailVerified", verified);
        body.put("active", active);
        return body;
    }

    private BookResponse createBook(String title, int quantity) {
        String isbn = nextIsbn();
        return bookService.create(new BookRequest(
                title, isbn, "Member search test", 2020, quantity,
                List.of("Member Search Author " + isbn), List.of("Member Search"), null, null, true));
    }

    private UserAccount createAccount(String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        UserAccount user = new UserAccount();
        user.setEmail(roleName.toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + "@member.test");
        user.setPasswordHash(passwordEncoder.encode("Password1"));
        user.setStatus(AccountStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        user.getRoles().add(role);
        MemberProfile profile = new MemberProfile();
        profile.setUserAccount(user);
        profile.setFullName("Member Test " + roleName);
        user.setProfile(profile);
        return userRepository.save(user);
    }

    private String tokenFor(UserAccount user) {
        return jwtService.createAccessToken(user).token();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String nextIsbn() {
        String first12 = "978" + String.format("%09d", ISBN_SEQUENCE.getAndIncrement());
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (first12.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return first12 + ((10 - sum % 10) % 10);
    }
}
