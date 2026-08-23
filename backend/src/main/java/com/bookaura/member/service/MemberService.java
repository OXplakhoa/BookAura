package com.bookaura.member.service;

import com.bookaura.account.entity.MemberProfile;
import com.bookaura.account.repository.MemberProfileRepository;
import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.auth.repository.RoleRepository;
import com.bookaura.auth.repository.UserAccountRepository;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.common.logging.LogOperation;
import com.bookaura.common.util.PhoneNormalizer;
import com.bookaura.common.web.PageResponse;
import com.bookaura.member.dto.*;
import com.bookaura.member.specification.MemberSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
@LogOperation
public class MemberService {

    private static final Logger AUDIT = LoggerFactory.getLogger("com.bookaura.audit");

    private final MemberProfileRepository memberRepository;
    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StrictMemberDateParser dateParser;
    private final MemberPageRequestFactory pageRequestFactory;
    private final MemberMapper mapper;

    public MemberService(MemberProfileRepository memberRepository, UserAccountRepository userRepository,
                         RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                         StrictMemberDateParser dateParser, MemberPageRequestFactory pageRequestFactory,
                         MemberMapper mapper) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.dateParser = dateParser;
        this.pageRequestFactory = pageRequestFactory;
        this.mapper = mapper;
    }

    @Transactional
    public MemberResponse create(MemberCreateRequest request) {
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

        UserAccount account = new UserAccount();
        account.setEmail(email);
        account.setPhone(phone);
        account.setPasswordHash(passwordEncoder.encode(request.initialPassword()));
        account.setStatus(Boolean.FALSE.equals(request.active()) ? AccountStatus.DISABLED : AccountStatus.ACTIVE);
        if (Boolean.TRUE.equals(request.emailVerified())) account.setEmailVerifiedAt(Instant.now());
        account.getRoles().add(userRole);

        MemberProfile member = new MemberProfile();
        member.setUserAccount(account);
        member.setFullName(request.fullName().trim());
        member.setDateOfBirth(request.dateOfBirth());
        member.setAddress(blankToNull(request.address()));
        account.setProfile(member);

        userRepository.save(account);
        AUDIT.info("event=MEMBER_CREATED memberId={} userId={}", member.getId(), account.getId());
        return mapper.toResponse(member);
    }

    @Transactional
    public MemberResponse update(UUID memberId, MemberUpdateRequest request) {
        MemberProfile member = findDetailed(memberId);
        UserAccount account = member.getUserAccount();
        String phone = PhoneNormalizer.normalize(request.phone());
        if (phone != null && userRepository.existsByPhoneAndIdNot(phone, account.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE, "Phone number is already registered");
        }

        member.setFullName(request.fullName().trim());
        member.setDateOfBirth(request.dateOfBirth());
        member.setAddress(blankToNull(request.address()));
        account.setPhone(phone);
        account.setStatus(request.active() ? AccountStatus.ACTIVE : AccountStatus.DISABLED);
        AUDIT.info("event=MEMBER_UPDATED memberId={} status={}", memberId, account.getStatus());
        return mapper.toResponse(member);
    }

    /** Idempotent disable; never deletes account/profile/loan history. */
    @Transactional
    public void disable(UUID memberId) {
        MemberProfile member = findDetailed(memberId);
        if (member.getUserAccount().getStatus() != AccountStatus.DISABLED) {
            member.getUserAccount().setStatus(AccountStatus.DISABLED);
            AUDIT.info("event=MEMBER_DISABLED memberId={} userId={}", memberId, member.getUserAccount().getId());
        }
    }

    @Transactional(readOnly = true)
    public MemberResponse get(UUID memberId) {
        return mapper.toResponse(findDetailed(memberId));
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> search(MemberSearchInput input, int page, int size, String sort) {
        LocalDate from = dateParser.parseOptional(input.dateOfBirthFrom(), "dateOfBirthFrom");
        LocalDate to = dateParser.parseOptional(input.dateOfBirthTo(), "dateOfBirthTo");
        dateParser.validateRange(from, to);
        MemberSearchCriteria criteria = new MemberSearchCriteria(
                input.name(), input.emailOrPhone(), from, to, input.borrowedBookTitle(),
                input.status(), input.role(), input.emailVerified());
        Page<MemberProfile> result = memberRepository.findAll(
                MemberSpecifications.from(criteria),
                pageRequestFactory.create(page, size, sort));
        return PageResponse.from(result, mapper::toResponse);
    }

    private MemberProfile findDetailed(UUID memberId) {
        return memberRepository.findDetailedById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND,
                        "Member not found: " + memberId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
