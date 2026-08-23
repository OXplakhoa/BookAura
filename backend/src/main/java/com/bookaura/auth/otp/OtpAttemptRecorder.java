package com.bookaura.auth.otp;

import com.bookaura.auth.entity.OtpToken;
import com.bookaura.auth.repository.OtpTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class OtpAttemptRecorder {

    private final OtpTokenRepository repository;

    public OtpAttemptRecorder(OtpTokenRepository repository) {
        this.repository = repository;
    }

    /** A rejected request throws afterward, so the security counter needs its own committed transaction. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID tokenId) {
        repository.incrementAttempt(tokenId, OtpToken.MAX_ATTEMPTS);
    }
}
