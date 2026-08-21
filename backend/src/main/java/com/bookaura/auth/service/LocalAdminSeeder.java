package com.bookaura.auth.service;

import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.RoleRepository;
import com.bookaura.auth.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * LOCAL/DEMO ONLY (D10): seeds admin/admin with a real BCrypt hash.
 * Never runs in prod (profile-gated); credentials come from env when overridden.
 */
@Component
@Profile("local")
public class LocalAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalAdminSeeder.class);

    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public LocalAdminSeeder(UserAccountRepository userRepository, RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${ADMIN_SEED_EMAIL:admin}") String adminEmail,
                            @Value("${ADMIN_SEED_PASSWORD:admin}") String adminPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        Role adminRole = roleRepository.findByName(Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role ADMIN not seeded"));
        Role userRole = roleRepository.findByName(Role.USER)
                .orElseThrow(() -> new IllegalStateException("Role USER not seeded"));

        UserAccount admin = new UserAccount();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setEmailVerifiedAt(Instant.now());
        admin.getRoles().add(adminRole);
        admin.getRoles().add(userRole);

        MemberProfile profile = new MemberProfile();
        profile.setUserAccount(admin);
        profile.setFullName("BookAura Administrator");
        admin.setProfile(profile);

        userRepository.save(admin);
        log.warn("LOCAL-ONLY seed: admin account '{}' created (never do this in prod)", adminEmail);
    }
}
