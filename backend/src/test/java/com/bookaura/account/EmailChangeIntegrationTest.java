package com.bookaura.account;

import com.bookaura.AbstractIntegrationTest;
import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.email.FakeEmailSender;
import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.OtpPurpose;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.OtpTokenRepository;
import com.bookaura.auth.repository.RoleRepository;
import com.bookaura.auth.repository.UserAccountRepository;
import com.bookaura.auth.token.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EmailChangeIntegrationTest extends AbstractIntegrationTest {

    private static final Pattern SIX_DIGITS = Pattern.compile("\\b(\\d{6})\\b");

    @Autowired private UserAccountRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private OtpTokenRepository otpTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private FakeEmailSender emailSender;

    @Test
    void emailChangesOnlyAfterCorrectCode_andCodeCannotReplay() throws Exception {
        UserAccount user = createUser();
        String newEmail = "changed-" + UUID.randomUUID() + "@email.test";
        String bearer = bearer(user);

        mockMvc.perform(post("/api/account/email-change/request")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("newEmail", newEmail))))
                .andExpect(status().isOk());
        assertThat(userRepository.findById(user.getId()).orElseThrow().getEmail()).isEqualTo(user.getEmail());
        String code = extractCode(emailSender.lastTo(newEmail).body());

        mockMvc.perform(post("/api/account/email-change/confirm")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("code", code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(newEmail))
                .andExpect(jsonPath("$.message").value("Email changed successfully."));
        assertThat(userRepository.findByEmail(newEmail)).isPresent();

        mockMvc.perform(post("/api/account/email-change/confirm")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("code", code))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_TOKEN_INVALID"));
    }

    @Test
    void fiveWrongCodesPersistAttempts_andLockEvenTheCorrectCode() throws Exception {
        UserAccount user = createUser();
        String newEmail = "attempts-" + UUID.randomUUID() + "@email.test";
        String bearer = bearer(user);
        requestChange(bearer, newEmail);
        String correctCode = extractCode(emailSender.lastTo(newEmail).body());
        String wrongCode = correctCode.equals("000000") ? "111111" : "000000";

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/account/email-change/confirm")
                            .header("Authorization", bearer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(java.util.Map.of("code", wrongCode))))
                    .andExpect(status().isBadRequest());
        }
        UserAccount reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(otpTokenRepository.findTopByUserAccountAndPurposeOrderByLastSentAtDesc(
                reloaded, OtpPurpose.CHANGE_EMAIL).orElseThrow().getAttempts()).isEqualTo(5);

        mockMvc.perform(post("/api/account/email-change/confirm")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("code", correctCode))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Too many failed attempts; request a new code"));
        assertThat(userRepository.findByEmail(newEmail)).isEmpty();
    }

    @Test
    void resendCooldownAndDuplicateEmailAreRejected() throws Exception {
        UserAccount user = createUser();
        UserAccount existing = createUser();
        String bearer = bearer(user);
        String newEmail = "cooldown-" + UUID.randomUUID() + "@email.test";
        requestChange(bearer, newEmail);

        mockMvc.perform(post("/api/account/email-change/request")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("newEmail", newEmail))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RESEND_COOLDOWN"));

        mockMvc.perform(post("/api/account/email-change/request")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("newEmail", existing.getEmail()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    private void requestChange(String bearer, String email) throws Exception {
        mockMvc.perform(post("/api/account/email-change/request")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("newEmail", email))))
                .andExpect(status().isOk());
    }

    private String bearer(UserAccount user) {
        return "Bearer " + jwtService.createAccessToken(user).token();
    }

    private UserAccount createUser() {
        Role role = roleRepository.findByName(Role.USER).orElseThrow();
        UserAccount user = new UserAccount();
        user.setEmail("account-" + UUID.randomUUID() + "@email.test");
        user.setPasswordHash(passwordEncoder.encode("EmailChange1"));
        user.setStatus(AccountStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        user.getRoles().add(role);
        MemberProfile profile = new MemberProfile();
        profile.setUserAccount(user);
        profile.setFullName("Email Change User");
        user.setProfile(profile);
        return userRepository.save(user);
    }

    private String extractCode(String body) {
        Matcher matcher = SIX_DIGITS.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
