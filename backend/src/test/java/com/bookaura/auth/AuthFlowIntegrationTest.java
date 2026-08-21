package com.bookaura.auth;

import com.bookaura.AbstractIntegrationTest;
import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.email.FakeEmailSender;
import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.RoleRepository;
import com.bookaura.auth.repository.UserAccountRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String COOKIE_NAME = "bookaura_refresh";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([A-Za-z0-9\\-_]+)");

    @Autowired
    private FakeEmailSender fakeEmailSender;
    @Autowired
    private UserAccountRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------- registration & verification ----------

    @Test
    void register_invalidPayload_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        register(email);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Other Name", email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    void login_beforeEmailVerification_returns401WithCode() throws Exception {
        String email = uniqueEmail();
        register(email);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void resendVerification_withinCooldown_returns429() throws Exception {
        String email = uniqueEmail();
        register(email); // sends the first verification email
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RESEND_COOLDOWN"));
    }

    // ---------- happy path: verify -> login -> me ----------

    @Test
    void fullFlow_verifyThenLogin_issuesAccessTokenAndRefreshCookie() throws Exception {
        String email = uniqueEmail();
        register(email);
        verifyEmail(email);

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.roles[0]").value("USER"))
                .andExpect(cookie().exists(COOKIE_NAME))
                .andExpect(cookie().httpOnly(COOKIE_NAME, true))
                .andReturn();

        String accessToken = accessToken(login);
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    // ---------- refresh rotation & reuse detection ----------

    @Test
    void refresh_rotatesToken_andReuseOfOldTokenRevokesWholeFamily() throws Exception {
        String email = uniqueEmail();
        register(email);
        verifyEmail(email);
        Cookie firstCookie = loginCookie(email);

        // Rotate: old cookie -> new cookie + new access token
        MvcResult rotated = mockMvc.perform(post("/api/auth/refresh").cookie(firstCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(cookie().exists(COOKIE_NAME))
                .andReturn();
        Cookie secondCookie = rotated.getResponse().getCookie(COOKIE_NAME);
        assertThat(secondCookie.getValue()).isNotEqualTo(firstCookie.getValue());

        // Attacker replays the OLD cookie -> reuse detected -> family revoked
        mockMvc.perform(post("/api/auth/refresh").cookie(firstCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_REUSED"));

        // Even the legitimately rotated cookie is now dead (family revoked)
        mockMvc.perform(post("/api/auth/refresh").cookie(secondCookie))
                .andExpect(status().isUnauthorized());
    }

    // ---------- logout revocation ----------

    @Test
    void logout_revokesAccessTokenJti_andRefreshSession() throws Exception {
        String email = uniqueEmail();
        register(email);
        verifyEmail(email);
        MvcResult login = login(email);
        String accessToken = accessToken(login);
        Cookie cookie = login.getResponse().getCookie(COOKIE_NAME);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(cookie))
                .andExpect(status().isNoContent());

        // The logged-out access token must not be reusable
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REVOKED"));

        // The refresh session is revoked too
        mockMvc.perform(post("/api/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    // ---------- role matrix ----------

    @Test
    void authorizationMatrix_anonymousUserAdmin() throws Exception {
        // anonymous -> 401
        mockMvc.perform(get("/api/admin/ping")).andExpect(status().isUnauthorized());

        // USER -> 403
        String email = uniqueEmail();
        register(email);
        verifyEmail(email);
        String userToken = accessToken(login(email));
        mockMvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // ADMIN -> 200
        String adminToken = accessToken(login(createAdmin()));
        mockMvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("admin-ok"));
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@test.dev";
    }

    private String registerJson(String fullName, String email) {
        return "{\"fullName\":\"" + fullName + "\",\"email\":\"" + email + "\",\"password\":\"Password1\"}";
    }

    private String loginJson(String identifier) {
        return "{\"identifier\":\"" + identifier + "\",\"password\":\"Password1\"}";
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Test User", email)))
                .andExpect(status().isCreated());
    }

    private void verifyEmail(String email) throws Exception {
        FakeEmailSender.SentMessage message = fakeEmailSender.lastTo(email);
        Matcher matcher = TOKEN_PATTERN.matcher(message.body());
        assertThat(matcher.find()).as("verification token present in email body").isTrue();
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + matcher.group(1) + "\"}"))
                .andExpect(status().isOk());
    }

    private MvcResult login(String identifier) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(identifier)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private Cookie loginCookie(String email) throws Exception {
        return login(email).getResponse().getCookie(COOKIE_NAME);
    }

    private String accessToken(MvcResult result) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return (String) body.get("accessToken");
    }

    private String createAdmin() {
        String email = uniqueEmail();
        Role adminRole = roleRepository.findByName(Role.ADMIN).orElseThrow();
        UserAccount admin = new UserAccount();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode("Password1"));
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setEmailVerifiedAt(Instant.now());
        admin.getRoles().add(adminRole);
        MemberProfile profile = new MemberProfile();
        profile.setUserAccount(admin);
        profile.setFullName("Test Admin");
        admin.setProfile(profile);
        userRepository.save(admin);
        return email;
    }
}
