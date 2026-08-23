package com.bookaura.auth.sms;

import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.OtpPurpose;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.otp.OtpTokenService;
import com.bookaura.auth.repository.UserAccountRepository;
import com.bookaura.auth.service.AuthService;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.common.logging.LogOperation;
import com.bookaura.common.util.PhoneNormalizer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@LogOperation
public class PhoneOtpService {

    private static final Logger AUDIT = LoggerFactory.getLogger("com.bookaura.audit");
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private final UserAccountRepository userRepository;
    private final OtpTokenService otpTokenService;
    private final SmsSender smsSender;
    private final AuthService authService;

    public PhoneOtpService(UserAccountRepository userRepository,
                           OtpTokenService otpTokenService,
                           SmsSender smsSender,
                           AuthService authService) {
        this.userRepository = userRepository;
        this.otpTokenService = otpTokenService;
        this.smsSender = smsSender;
        this.authService = authService;
    }

    /** Enumeration-safe for missing/disabled numbers. Existing-account cooldown remains enforced silently. */
    public void request(String rawPhone) {
        String phone = PhoneNormalizer.normalize(rawPhone);
        userRepository.findByPhone(phone)
                .filter(user -> user.getStatus() == AccountStatus.ACTIVE)
                .ifPresent(user -> {
                    try {
                        String code = otpTokenService.createToken(
                                user, phone, OtpPurpose.PHONE_LOGIN, CODE_TTL, true);
                        smsSender.sendOtp(phone, code);
                        AUDIT.info("event=PHONE_OTP_SENT userId={}", user.getId());
                    } catch (BusinessException exception) {
                        if (exception.code() != ErrorCode.RESEND_COOLDOWN) throw exception;
                        AUDIT.info("event=PHONE_OTP_COOLDOWN userId={}", user.getId());
                    }
                });
    }

    @Transactional
    public AuthService.LoginResult confirm(String rawPhone, String code, HttpServletRequest request) {
        String phone = PhoneNormalizer.normalize(rawPhone);
        UserAccount user = userRepository.findByPhone(phone)
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS,
                        "Invalid phone or verification code"));
        otpTokenService.consumeLatestCode(user, OtpPurpose.PHONE_LOGIN, code);
        return authService.loginFromPhoneOtp(user.getId(), request);
    }
}
