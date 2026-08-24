package com.bookaura.auth.oauth;

import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.RoleRepository;
import com.bookaura.auth.repository.UserAccountRepository;
import com.bookaura.auth.service.AuthService;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.common.logging.LogOperation;
import com.bookaura.common.util.HashUtils;
import com.bookaura.common.util.TokenGenerator;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@LogOperation
public class OAuthLoginService {

    private static final Logger AUDIT = LoggerFactory.getLogger("com.bookaura.audit");
    private static final Duration EXCHANGE_TTL = Duration.ofSeconds(60);

    private final OAuthIdentityRepository identityRepository;
    private final OAuthExchangeCodeRepository codeRepository;
    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public OAuthLoginService(OAuthIdentityRepository identityRepository,
                             OAuthExchangeCodeRepository codeRepository,
                             UserAccountRepository userRepository,
                             RoleRepository roleRepository,
                             PasswordEncoder passwordEncoder,
                             AuthService authService) {
        this.identityRepository = identityRepository;
        this.codeRepository = codeRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    /** Called only after Spring Security has validated Google's OIDC signature/state/nonce. */
    @Transactional
    public String beginGoogleLogin(GoogleClaims claims) {
        validateClaims(claims);
        return beginLogin(OAuthProvider.GOOGLE, claims.subject(), claims.email(), claims.fullName());
    }

    /**
     * Called after Spring Security completes the Facebook authorization-code exchange and loads
     * Graph API {@code /me}. Facebook has no email_verified claim; Graph API emails are treated as
     * verified (D29). Email may be entirely absent (denied permission / phone-signed-up account).
     */
    @Transactional
    public String beginFacebookLogin(FacebookClaims claims) {
        if (!StringUtils.hasText(claims.subject()) || !StringUtils.hasText(claims.email())) {
            throw new BusinessException(ErrorCode.OAUTH_EMAIL_NOT_VERIFIED,
                    "Facebook did not provide an email address");
        }
        return beginLogin(OAuthProvider.FACEBOOK, claims.subject(), claims.email(), claims.fullName());
    }

    private String beginLogin(OAuthProvider provider, String subject, String email, String fullName) {
        UserAccount user = identityRepository
                .findByProviderAndProviderSubject(provider, subject)
                .map(OAuthIdentity::getUserAccount)
                .orElseGet(() -> linkOrCreateIdentity(provider, subject, email, fullName));
        requireActive(user);

        String rawCode = TokenGenerator.urlSafeToken();
        OAuthExchangeCode code = new OAuthExchangeCode();
        code.setCodeHash(HashUtils.sha256Hex(rawCode));
        code.setUserAccount(user);
        code.setExpiresAt(Instant.now().plus(EXCHANGE_TTL));
        codeRepository.save(code);
        AUDIT.info("event=OAUTH_CALLBACK_SUCCESS provider={} userId={}", provider, user.getId());
        return rawCode;
    }

    /** Atomic NULL -> consumed update makes the redirect code single-use under concurrent exchanges. */
    @Transactional
    public AuthService.LoginResult exchange(String rawCode, HttpServletRequest request) {
        if (!StringUtils.hasText(rawCode)) {
            throw invalidExchange();
        }
        String hash = HashUtils.sha256Hex(rawCode);
        Instant now = Instant.now();
        if (codeRepository.consumeIfUsable(hash, now) != 1) {
            throw invalidExchange();
        }
        OAuthExchangeCode code = codeRepository.findByCodeHash(hash).orElseThrow(this::invalidExchange);
        return authService.loginFromOAuthExchange(code.getUserAccount().getId(), request);
    }

    private UserAccount linkOrCreateIdentity(OAuthProvider provider, String subject, String rawEmail, String fullName) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        UserAccount user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email, fullName));
        requireActive(user);
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(Instant.now());
        }

        OAuthIdentity identity = new OAuthIdentity();
        identity.setProvider(provider);
        identity.setProviderSubject(subject);
        identity.setUserAccount(user);
        identityRepository.save(identity);
        AUDIT.info("event=OAUTH_IDENTITY_LINKED provider={} userId={}", provider, user.getId());
        return user;
    }

    private UserAccount createUser(String email, String fullName) {
        Role userRole = roleRepository.findByName(Role.USER)
                .orElseThrow(() -> new IllegalStateException("Role USER not seeded"));
        UserAccount user = new UserAccount();
        user.setEmail(email);
        // OAuth-only accounts receive an unguessable BCrypt value; password login cannot use it.
        user.setPasswordHash(passwordEncoder.encode(TokenGenerator.urlSafeToken()));
        user.setEmailVerifiedAt(Instant.now());
        user.setStatus(AccountStatus.ACTIVE);
        user.getRoles().add(userRole);

        MemberProfile profile = new MemberProfile();
        profile.setUserAccount(user);
        profile.setFullName(StringUtils.hasText(fullName) ? fullName.trim() : email);
        user.setProfile(profile);
        return userRepository.save(user);
    }

    private void validateClaims(GoogleClaims claims) {
        if (!StringUtils.hasText(claims.subject()) || !StringUtils.hasText(claims.email()) || !claims.emailVerified()) {
            throw new BusinessException(ErrorCode.OAUTH_EMAIL_NOT_VERIFIED,
                    "Google did not provide a verified email address");
        }
    }

    private void requireActive(UserAccount user) {
        if (user.getStatus() == AccountStatus.DISABLED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Account is disabled");
        }
    }

    private BusinessException invalidExchange() {
        return new BusinessException(ErrorCode.OAUTH_EXCHANGE_INVALID,
                "OAuth exchange code is invalid, expired, or already used");
    }

    public record GoogleClaims(String subject, String email, boolean emailVerified, String fullName) {
    }

    public record FacebookClaims(String subject, String email, String fullName) {
    }
}
