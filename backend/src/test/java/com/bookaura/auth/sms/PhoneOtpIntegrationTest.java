package com.bookaura.auth.sms;

import com.bookaura.AbstractIntegrationTest;
import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.RoleRepository;
import com.bookaura.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PhoneOtpIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserAccountRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private FakeSmsSender smsSender;

    @Test
    void activePhoneAccountCanLogin_andCodeCannotReplay() throws Exception {
        UserAccount user = createUser(AccountStatus.ACTIVE);
        requestCode(user.getPhone());
        String code = smsSender.lastTo(user.getPhone()).code();

        mockMvc.perform(post("/api/auth/phone-otp/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("phone", user.getPhone(), "code", code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.id").value(user.getId().toString()))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));

        mockMvc.perform(post("/api/auth/phone-otp/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("phone", user.getPhone(), "code", code))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_TOKEN_INVALID"));
    }

    @Test
    void repeatedRequestIsEnumerationSafe_andCooldownSendsOnlyOnce() throws Exception {
        UserAccount user = createUser(AccountStatus.ACTIVE);
        long before = sentCount(user.getPhone());

        requestCode(user.getPhone());
        requestCode(user.getPhone());
        assertThat(sentCount(user.getPhone()) - before).isEqualTo(1);

        mockMvc.perform(post("/api/auth/phone-otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("phone", "+84999999999"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "If an active account uses this phone, a code was sent. Please wait before retrying."));
    }

    @Test
    void disabledAccountDoesNotReceiveCode() throws Exception {
        UserAccount user = createUser(AccountStatus.DISABLED);
        long before = sentCount(user.getPhone());
        requestCode(user.getPhone());
        assertThat(sentCount(user.getPhone())).isEqualTo(before);
    }

    private void requestCode(String phone) throws Exception {
        mockMvc.perform(post("/api/auth/phone-otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("phone", phone))))
                .andExpect(status().isOk());
    }

    private long sentCount(String phone) {
        return smsSender.sentMessages().stream().filter(message -> message.phone().equals(phone)).count();
    }

    private UserAccount createUser(AccountStatus status) {
        String suffix = UUID.randomUUID().toString();
        Role role = roleRepository.findByName(Role.USER).orElseThrow();
        UserAccount user = new UserAccount();
        user.setEmail("phone-" + suffix + "@otp.test");
        user.setPhone(String.format("+849%09d", Math.floorMod(suffix.hashCode(), 1_000_000_000)));
        user.setPasswordHash(passwordEncoder.encode("PhoneOtpPass1"));
        user.setStatus(status);
        // Deliberately no emailVerifiedAt: possession of the registered phone is this flow's proof.
        user.getRoles().add(role);
        MemberProfile profile = new MemberProfile();
        profile.setUserAccount(user);
        profile.setFullName("Phone OTP User");
        user.setProfile(profile);
        return userRepository.save(user);
    }
}
