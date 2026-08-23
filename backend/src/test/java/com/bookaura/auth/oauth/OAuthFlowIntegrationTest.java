package com.bookaura.auth.oauth;

import com.bookaura.AbstractIntegrationTest;
import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.RoleRepository;
import com.bookaura.auth.repository.UserAccountRepository;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OAuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private OAuthLoginService oauthLoginService;
    @Autowired private OAuthIdentityRepository identityRepository;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void googleCallbackCreatesVerifiedUser_andExchangeCodeIsSingleUse() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String email = "google-" + suffix + "@oauth.test";
        String rawCode = oauthLoginService.beginGoogleLogin(
                new OAuthLoginService.GoogleClaims("google-sub-" + suffix, email, true, "Google Reader"));

        mockMvc.perform(post("/api/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("code", rawCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.fullName").value("Google Reader"))
                .andExpect(jsonPath("$.user.roles[0]").value("USER"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));

        mockMvc.perform(post("/api/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("code", rawCode))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OAUTH_EXCHANGE_INVALID"));

        UserAccount user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(identityRepository.findByProviderAndProviderSubject(
                OAuthProvider.GOOGLE, "google-sub-" + suffix)).isPresent();
    }

    @Test
    void googleCallbackLinksExistingEmailAccount_withoutCreatingDuplicate() {
        String suffix = UUID.randomUUID().toString();
        String email = "existing-" + suffix + "@oauth.test";
        UserAccount existing = createUnverifiedUser(email);

        oauthLoginService.beginGoogleLogin(
                new OAuthLoginService.GoogleClaims("linked-sub-" + suffix, email, true, "Provider Name"));

        UserAccount linked = userRepository.findByEmail(email).orElseThrow();
        OAuthIdentity identity = identityRepository.findByProviderAndProviderSubject(
                OAuthProvider.GOOGLE, "linked-sub-" + suffix).orElseThrow();
        assertThat(linked.getId()).isEqualTo(existing.getId());
        assertThat(linked.getEmailVerifiedAt()).isNotNull();
        assertThat(identity.getUserAccount().getId()).isEqualTo(existing.getId());
    }

    @Test
    void unverifiedProviderEmailIsRejected_andProviderAvailabilityIsFalseWithoutCredentials() throws Exception {
        assertThatThrownBy(() -> oauthLoginService.beginGoogleLogin(
                new OAuthLoginService.GoogleClaims("subject", "unverified@oauth.test", false, "Reader")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.code()).isEqualTo(ErrorCode.OAUTH_EMAIL_NOT_VERIFIED));

        mockMvc.perform(get("/api/auth/oauth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.google").value(false));
    }

    private UserAccount createUnverifiedUser(String email) {
        Role userRole = roleRepository.findByName(Role.USER).orElseThrow();
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("ExistingPass1"));
        user.setStatus(AccountStatus.ACTIVE);
        user.getRoles().add(userRole);
        MemberProfile profile = new MemberProfile();
        profile.setUserAccount(user);
        profile.setFullName("Existing Reader");
        user.setProfile(profile);
        return userRepository.save(user);
    }
}
