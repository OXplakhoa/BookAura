package com.bookaura.auth.sms;

import com.bookaura.AbstractIntegrationTest;
import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.entity.OtpPurpose;
import com.bookaura.auth.repository.OtpTokenRepository;
import com.bookaura.auth.repository.RoleRepository;
import com.bookaura.auth.repository.UserAccountRepository;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(PhoneOtpDeliveryFailureIntegrationTest.FailingSmsConfiguration.class)
class PhoneOtpDeliveryFailureIntegrationTest extends AbstractIntegrationTest {

    @Autowired private PhoneOtpService phoneOtpService;
    @Autowired private OtpTokenRepository otpTokenRepository;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void knownProviderFailureRollsBackNewOtpToken() {
        UserAccount user = createUser();

        assertThatThrownBy(() -> phoneOtpService.request(user.getPhone()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.SMS_DELIVERY_UNAVAILABLE));

        assertThat(otpTokenRepository.findTopByUserAccountAndPurposeOrderByLastSentAtDesc(
                user, OtpPurpose.PHONE_LOGIN)).isEmpty();
    }

    private UserAccount createUser() {
        Role role = roleRepository.findByName(Role.USER).orElseThrow();
        UserAccount user = new UserAccount();
        user.setEmail("sms-failure-" + UUID.randomUUID() + "@otp.test");
        user.setPhone("+849" + String.format("%09d", Math.floorMod(UUID.randomUUID().hashCode(), 1_000_000_000)));
        user.setPasswordHash(passwordEncoder.encode("PhoneOtpPass1"));
        user.setStatus(AccountStatus.ACTIVE);
        user.getRoles().add(role);
        MemberProfile profile = new MemberProfile();
        profile.setUserAccount(user);
        profile.setFullName("SMS Failure User");
        user.setProfile(profile);
        return userRepository.save(user);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingSmsConfiguration {
        @Bean
        @Primary
        SmsSender failingSmsSender() {
            return (phone, code) -> {
                throw new BusinessException(ErrorCode.SMS_DELIVERY_UNAVAILABLE,
                        "SMS delivery is unavailable");
            };
        }
    }
}
