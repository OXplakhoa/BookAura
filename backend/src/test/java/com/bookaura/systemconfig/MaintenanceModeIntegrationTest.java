package com.bookaura.systemconfig;

import com.bookaura.AbstractIntegrationTest;
import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.RoleRepository;
import com.bookaura.auth.repository.UserAccountRepository;
import com.bookaura.auth.token.JwtService;
import com.bookaura.systemconfig.service.SystemConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MaintenanceModeIntegrationTest extends AbstractIntegrationTest {

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private SystemConfigurationService configurationService;

    private UserAccount admin;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        admin = createAccount(Role.ADMIN);
        adminToken = jwtService.createAccessToken(admin).token();
        userToken = jwtService.createAccessToken(createAccount(Role.USER)).token();
        configurationService.setMaintenanceMode(false, admin.getId());
    }

    @AfterEach
    void ensureMaintenanceOff() {
        configurationService.setMaintenanceMode(false, admin.getId());
    }

    @Test
    void maintenanceBlocksNormalApis_butAdminControlAndHealthRemainAvailable() throws Exception {
        mockMvc.perform(put("/api/admin/system-config/maintenance")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintenanceMode").value(true));

        // Public and authenticated business APIs both return the same 503 contract.
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value("MAINTENANCE_MODE"))
                .andExpect(jsonPath("$.traceId").exists());
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(userToken)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("MAINTENANCE_MODE"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/api/admin/system-config").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintenanceMode").value(true));

        // Filter bypass is operational only; Spring Security still enforces ADMIN.
        mockMvc.perform(put("/api/admin/system-config/maintenance")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/system-config"))
                .andExpect(status().isUnauthorized());

        // CORS preflight is allowed; actual business request remains blocked above.
        mockMvc.perform(options("/api/books")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/system-config/maintenance")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintenanceMode").value(false));
        mockMvc.perform(get("/api/books")).andExpect(status().isOk());
    }

    @Test
    void maintenanceRequest_requiresEnabledField() throws Exception {
        mockMvc.perform(put("/api/admin/system-config/maintenance")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.enabled").exists());
    }

    private UserAccount createAccount(String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        UserAccount account = new UserAccount();
        account.setEmail(roleName.toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + "@maintenance.test");
        account.setPasswordHash(passwordEncoder.encode("Password1"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setEmailVerifiedAt(Instant.now());
        account.getRoles().add(role);
        MemberProfile profile = new MemberProfile();
        profile.setUserAccount(account);
        profile.setFullName("Maintenance Test " + roleName);
        account.setProfile(profile);
        return userRepository.save(account);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
