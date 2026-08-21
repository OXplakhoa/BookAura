package com.bookaura.auth.repository;

import com.bookaura.auth.entity.OtpPurpose;
import com.bookaura.auth.entity.OtpToken;
import com.bookaura.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    Optional<OtpToken> findByCodeHashAndPurpose(String codeHash, OtpPurpose purpose);

    /** Latest token for cooldown checks. */
    Optional<OtpToken> findTopByUserAccountAndPurposeOrderByLastSentAtDesc(UserAccount userAccount, OtpPurpose purpose);
}
