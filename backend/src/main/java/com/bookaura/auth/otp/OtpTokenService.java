package com.bookaura.auth.otp;

import com.bookaura.auth.entity.OtpPurpose;
import com.bookaura.auth.entity.OtpToken;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.OtpTokenRepository;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.common.util.HashUtils;
import com.bookaura.common.util.TokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Issues and consumes one-time secrets for every verification flow.
 * Guarantees: hashed storage, expiry, one-time consumption, attempt limit, resend cooldown, purpose binding.
 */
@Service
public class OtpTokenService {

    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final OtpTokenRepository repository;
    private final OtpAttemptRecorder attemptRecorder;

    public OtpTokenService(OtpTokenRepository repository, OtpAttemptRecorder attemptRecorder) {
        this.repository = repository;
        this.attemptRecorder = attemptRecorder;
    }

    /** Creates a new secret (enforcing resend cooldown) and returns the RAW value to deliver out-of-band. */
    @Transactional
    public String createToken(UserAccount user, String target, OtpPurpose purpose, Duration ttl, boolean sixDigitCode) {
        repository.findTopByUserAccountAndPurposeOrderByLastSentAtDesc(user, purpose)
                .filter(latest -> latest.getLastSentAt().plus(RESEND_COOLDOWN).isAfter(Instant.now()))
                .ifPresent(latest -> {
                    throw new BusinessException(ErrorCode.RESEND_COOLDOWN,
                            "Please wait before requesting another code");
                });

        String raw = sixDigitCode ? TokenGenerator.sixDigitCode() : TokenGenerator.urlSafeToken();
        OtpToken token = new OtpToken();
        token.setUserAccount(user);
        token.setTarget(target);
        token.setPurpose(purpose);
        token.setCodeHash(HashUtils.sha256Hex(raw));
        token.setExpiresAt(Instant.now().plus(ttl));
        token.setLastSentAt(Instant.now());
        repository.save(token);
        return raw;
    }

    /** Looks up the token by raw value + purpose and validates state. Caller must then markConsumed(). */
    @Transactional
    public OtpToken validate(String raw, OtpPurpose purpose) {
        OtpToken token = repository.findByCodeHashAndPurpose(HashUtils.sha256Hex(raw), purpose)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_TOKEN_INVALID,
                        "Invalid or unknown verification code"));

        validateState(token);
        return token;
    }

    /**
     * Authenticated six-digit flow: bind validation to the current user and latest purpose token.
     * Wrong attempts commit in REQUIRES_NEW; correct consumption is one atomic conditional update.
     */
    @Transactional
    public ConsumedOtp consumeLatestCode(UserAccount user, OtpPurpose purpose, String rawCode) {
        OtpToken token = repository.findTopByUserAccountAndPurposeOrderByLastSentAtDesc(user, purpose)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_TOKEN_INVALID,
                        "Invalid or unknown verification code"));
        validateState(token);
        String suppliedHash = HashUtils.sha256Hex(rawCode == null ? "" : rawCode.trim());
        if (!HashUtils.constantTimeEquals(token.getCodeHash(), suppliedHash)) {
            attemptRecorder.record(token.getId());
            throw new BusinessException(ErrorCode.VERIFICATION_TOKEN_INVALID, "Invalid verification code");
        }
        Instant now = Instant.now();
        int consumed = repository.consumeIfUsable(token.getId(), user.getId(), purpose, suppliedHash,
                now, OtpToken.MAX_ATTEMPTS);
        if (consumed != 1) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOKEN_INVALID,
                    "This code is invalid, expired, or already used");
        }
        return new ConsumedOtp(token.getId(), token.getTarget());
    }

    @Transactional
    public void markConsumed(OtpToken token) {
        token.setConsumedAt(Instant.now());
    }

    private void validateState(OtpToken token) {
        if (token.getConsumedAt() != null) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOKEN_INVALID, "This code has already been used");
        }
        if (!token.getExpiresAt().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOKEN_EXPIRED, "This code has expired");
        }
        if (token.getAttempts() >= OtpToken.MAX_ATTEMPTS) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOKEN_INVALID,
                    "Too many failed attempts; request a new code");
        }
    }

    public record ConsumedOtp(UUID tokenId, String target) {
    }
}
