package com.bookaura.common.logging;

import com.bookaura.AbstractIntegrationTest;
import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.RoleRepository;
import com.bookaura.auth.repository.UserAccountRepository;
import com.bookaura.auth.token.JwtService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObservabilityIntegrationTest extends AbstractIntegrationTest {

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    @Test
    void logsHttpMetadataAndServiceTiming_withoutPasswordOrTokenLeak() throws Exception {
        CapturingAppender httpAppender = attach("http-test", HttpExchangeLoggingFilter.class);
        CapturingAppender serviceAppender = attach("service-test", ServiceOperationLoggingAspect.class);
        try {
            UserAccount admin = createAccount(Role.ADMIN, "AdminPassword1");
            String adminToken = jwtService.createAccessToken(admin).token();
            String memberPassword = "NeverLogThis123";
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            String memberEmail = "observed-" + suffix + "@test.dev";
            String memberPhone = "+8491234" + Math.abs(suffix.hashCode()) % 1000;

            String memberBody = """
                    {"fullName":"Observed Member %s","email":"%s",
                    "phone":"%s","initialPassword":"%s","dateOfBirth":"1995-02-09",
                    "address":"Observed","emailVerified":true,"active":true}
                    """.formatted(suffix, memberEmail, memberPhone, memberPassword);
            mockMvc.perform(post("/api/admin/members")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(memberBody))
                    .andExpect(status().isCreated());

            // Auth path body and response are fully suppressed, including the access token.
            UserAccount loginUser = createAccount(Role.USER, "LoginSecret123");
            String loginBody = "{\"identifier\":\"" + loginUser.getEmail()
                    + "\",\"password\":\"LoginSecret123\"}";
            String response = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String issuedToken = objectMapper.readTree(response).get("accessToken").asText();

            String logs = String.join("\n", httpAppender.messages()) + "\n"
                    + String.join("\n", serviceAppender.messages());
            assertThat(logs)
                    .contains("http_exchange method=POST path=/api/admin/members status=201")
                    .contains("service_operation operation=MemberService.create outcome=SUCCESS")
                    .contains("http_exchange method=POST path=/api/auth/login status=200")
                    .contains("requestBody=<suppressed> responseBody=<suppressed>")
                    .doesNotContain(memberPassword, memberEmail, memberPhone,
                            "LoginSecret123", issuedToken, adminToken);
        } finally {
            detach(httpAppender, HttpExchangeLoggingFilter.class);
            detach(serviceAppender, ServiceOperationLoggingAspect.class);
        }
    }

    private CapturingAppender attach(String name, Class<?> loggerType) {
        CapturingAppender appender = new CapturingAppender(name);
        appender.start();
        ((org.apache.logging.log4j.core.Logger) LogManager.getLogger(loggerType)).addAppender(appender);
        return appender;
    }

    private void detach(CapturingAppender appender, Class<?> loggerType) {
        ((org.apache.logging.log4j.core.Logger) LogManager.getLogger(loggerType)).removeAppender(appender);
        appender.stop();
    }

    private UserAccount createAccount(String roleName, String rawPassword) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        UserAccount user = new UserAccount();
        user.setEmail(roleName.toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + "@logging.test");
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(AccountStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        user.getRoles().add(role);
        MemberProfile profile = new MemberProfile();
        profile.setUserAccount(user);
        profile.setFullName("Logging Test " + roleName);
        profile.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setProfile(profile);
        return userRepository.save(user);
    }

    static class CapturingAppender extends AbstractAppender {
        private final List<String> messages = new CopyOnWriteArrayList<>();

        CapturingAppender(String name) {
            super(name, null, null, false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            messages.add(event.getMessage().getFormattedMessage());
        }

        List<String> messages() {
            return List.copyOf(messages);
        }
    }
}
