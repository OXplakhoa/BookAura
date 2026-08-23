package com.bookaura.account.service;

import com.bookaura.account.dto.EmailChangeResponse;
import com.bookaura.auth.dto.AuthResponse;
import com.bookaura.auth.email.EmailSender;
import com.bookaura.auth.entity.OtpPurpose;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.otp.OtpTokenService;
import com.bookaura.auth.repository.UserAccountRepository;
import com.bookaura.auth.service.AuthService;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.common.logging.LogOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@LogOperation
public class EmailChangeService {

    private static final Logger AUDIT = LoggerFactory.getLogger("com.bookaura.audit");
    private static final Duration CODE_TTL = Duration.ofMinutes(10);

    private final UserAccountRepository userRepository;
    private final OtpTokenService otpTokenService;
    private final EmailSender emailSender;

    public EmailChangeService(UserAccountRepository userRepository,
                              OtpTokenService otpTokenService,
                              EmailSender emailSender) {
        this.userRepository = userRepository;
        this.otpTokenService = otpTokenService;
        this.emailSender = emailSender;
    }

    @Transactional
    public void request(UUID userId, String rawNewEmail) {
        UserAccount user = findUser(userId);
        String newEmail = normalize(rawNewEmail);
        if (newEmail.equals(user.getEmail()) || userRepository.existsByEmail(newEmail)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "Email is already registered");
        }
        String code = otpTokenService.createToken(user, newEmail, OtpPurpose.CHANGE_EMAIL, CODE_TTL, true);
        emailSender.send(newEmail, "Confirm your new BookAura email",
                "Your BookAura email change code is: " + code
                        + "\n\nIt expires in 10 minutes. If you did not request this, ignore this email.");
        AUDIT.info("event=EMAIL_CHANGE_REQUESTED userId={}", user.getId());
    }

    @Transactional
    public EmailChangeResponse confirm(UUID userId, String code) {
        UserAccount user = findUser(userId);
        OtpTokenService.ConsumedOtp consumed = otpTokenService.consumeLatestCode(
                user, OtpPurpose.CHANGE_EMAIL, code);
        String newEmail = normalize(consumed.target());
        if (userRepository.existsByEmail(newEmail)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "Email is already registered");
        }
        user.setEmail(newEmail);
        user.setEmailVerifiedAt(Instant.now());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "Email is already registered");
        }
        AUDIT.info("event=EMAIL_CHANGED userId={}", user.getId());
        AuthResponse.UserSummary summary = AuthService.toSummary(user);
        return new EmailChangeResponse("Email changed successfully.", summary);
    }

    private UserAccount findUser(UUID userId) {
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Account not found"));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
