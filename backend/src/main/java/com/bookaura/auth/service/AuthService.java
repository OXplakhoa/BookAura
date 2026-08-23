package com.bookaura.auth.service;

import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.dto.*;
import com.bookaura.auth.email.EmailSender;
import com.bookaura.auth.entity.*;
import com.bookaura.auth.otp.OtpTokenService;
import com.bookaura.auth.repository.RoleRepository;
import com.bookaura.auth.repository.RevokedAccessTokenRepository;
import com.bookaura.auth.repository.UserAccountRepository;
import com.bookaura.auth.token.JwtService;
import com.bookaura.auth.token.RefreshTokenService;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.common.logging.LogOperation;
import com.bookaura.common.util.PhoneNormalizer;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Auth use-cases. Transaction boundary lives here (service layer), never in controllers.
 * Audit log lines use the dedicated "com.bookaura.audit" logger and never contain secrets.
 */
@Service
@LogOperation
public class AuthService {

    private static final Logger AUDIT = LoggerFactory.getLogger("com.bookaura.audit");
    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);

    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final RevokedAccessTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OtpTokenService otpTokenService;
    private final EmailSender emailSender;
    private final String frontendUrl;

    public AuthService(UserAccountRepository userRepository, RoleRepository roleRepository,
                       RevokedAccessTokenRepository revokedTokenRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, RefreshTokenService refreshTokenService,
                       OtpTokenService otpTokenService, EmailSender emailSender,
                       @Value("${bookaura.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.otpTokenService = otpTokenService;
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "Email is already registered");
        }
        String phone = PhoneNormalizer.normalize(request.phone());
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE, "Phone number is already registered");
        }

        Role userRole = roleRepository.findByName(Role.USER)
                .orElseThrow(() -> new IllegalStateException("Role USER not seeded"));

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(AccountStatus.ACTIVE);
        user.getRoles().add(userRole);

        MemberProfile profile = new MemberProfile();
        profile.setUserAccount(user);
        profile.setFullName(request.fullName().trim());
        user.setProfile(profile);

        userRepository.save(user);

        String rawToken = otpTokenService.createToken(user, email, OtpPurpose.EMAIL_VERIFICATION,
                VERIFICATION_TTL, false);
        emailSender.sendVerificationEmail(email, frontendUrl + "/verify-email?token=" + rawToken);
        AUDIT.info("event=REGISTER userId={}", user.getId());
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        OtpToken token = otpTokenService.validate(rawToken, OtpPurpose.EMAIL_VERIFICATION);
        UserAccount user = token.getUserAccount();
        if (user == null) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOKEN_INVALID, "Token is not bound to an account");
        }
        user.setEmailVerifiedAt(Instant.now());
        otpTokenService.markConsumed(token);
        AUDIT.info("event=EMAIL_VERIFIED userId={}", user.getId());
    }

    /** Enumeration-safe: always succeeds from the client's perspective. */
    @Transactional
    public void resendVerification(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmail(email)
                .filter(user -> user.getEmailVerifiedAt() == null)
                .ifPresent(user -> {
                    String rawToken = otpTokenService.createToken(user, email, OtpPurpose.EMAIL_VERIFICATION,
                            VERIFICATION_TTL, false);
                    emailSender.sendVerificationEmail(email, frontendUrl + "/verify-email?token=" + rawToken);
                    AUDIT.info("event=VERIFICATION_RESENT userId={}", user.getId());
                });
    }

    @Transactional
    public LoginResult login(LoginRequest request, HttpServletRequest http) {
        UserAccount user = findByIdentifier(request.identifier());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // Deliberately identical message: do not reveal which part failed.
            AUDIT.info("event=LOGIN_FAILED reason=INVALID_CREDENTIALS");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }
        if (user.getEmailVerifiedAt() == null) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED,
                    "Email is not verified. Please check your inbox.");
        }
        if (user.getStatus() == AccountStatus.DISABLED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Account is disabled");
        }

        JwtService.IssuedToken access = jwtService.createAccessToken(user);
        RefreshTokenService.IssuedSession refresh = refreshTokenService.issue(user, http.getRemoteAddr(),
                http.getHeader("User-Agent"));
        AUDIT.info("event=LOGIN_SUCCESS userId={}", user.getId());
        return new LoginResult(access, refresh.rawToken(), refresh.expiresAt(), toSummary(user));
    }

    @Transactional
    public LoginResult refresh(String rawRefreshToken, HttpServletRequest http) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_MISSING, "Refresh cookie is missing");
        }
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(
                rawRefreshToken, http.getRemoteAddr(), http.getHeader("User-Agent"));
        UserAccount user = userRepository.findWithRolesById(rotation.userAccount().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_INVALID, "Account not found"));
        JwtService.IssuedToken access = jwtService.createAccessToken(user);
        return new LoginResult(access, rotation.session().rawToken(), rotation.session().expiresAt(), toSummary(user));
    }

    /**
     * Logout = revoke refresh session (cookie) + blacklist the current access token's jti
     * until its natural expiry, so it cannot be replayed (D4).
     */
    @Transactional
    public void logout(String authorizationHeader, String rawRefreshToken) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(authorizationHeader.substring(7));
                if (!revokedTokenRepository.existsById(claims.getId())) {
                    RevokedAccessToken revoked = new RevokedAccessToken();
                    revoked.setJti(claims.getId());
                    revoked.setExpiresAt(claims.getExpiration().toInstant());
                    revokedTokenRepository.save(revoked);
                }
            } catch (Exception ignored) {
                // Token already invalid/expired: nothing to blacklist.
            }
        }
        refreshTokenService.revoke(rawRefreshToken);
        AUDIT.info("event=LOGOUT");
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserSummary me(UUID userId) {
        UserAccount user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Account not found"));
        return toSummary(user);
    }

    private UserAccount findByIdentifier(String identifier) {
        // Try email first (covers non-standard local identities like the seeded "admin"),
        // then fall back to normalized phone. Never reveals which lookup failed.
        String email = identifier.trim().toLowerCase(Locale.ROOT);
        var byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            return byEmail.get();
        }
        String phone = PhoneNormalizer.normalize(identifier);
        return phone == null ? null : userRepository.findByPhone(phone).orElse(null);
    }

    public static AuthResponse.UserSummary toSummary(UserAccount user) {
        return new AuthResponse.UserSummary(
                user.getId(),
                user.getEmail(),
                user.getProfile() != null ? user.getProfile().getFullName() : null,
                user.getRoles().stream().map(Role::getName).sorted().toList());
    }

    public record LoginResult(JwtService.IssuedToken access, String rawRefreshToken,
                              Instant refreshExpiresAt, AuthResponse.UserSummary user) {
    }
}
