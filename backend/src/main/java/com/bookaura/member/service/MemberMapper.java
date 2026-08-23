package com.bookaura.member.service;

import com.bookaura.account.entity.MemberProfile;
import com.bookaura.auth.entity.Role;
import com.bookaura.auth.entity.UserAccount;
import com.bookaura.member.dto.MemberResponse;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public MemberResponse toResponse(MemberProfile member) {
        UserAccount user = member.getUserAccount();
        return new MemberResponse(
                member.getId(),
                user.getId(),
                member.getFullName(),
                user.getEmail(),
                user.getPhone(),
                member.getDateOfBirth(),
                member.getAddress(),
                user.getStatus().name(),
                user.getEmailVerifiedAt() != null,
                user.getRoles().stream().map(Role::getName).sorted().toList(),
                member.getCreatedAt(),
                member.getUpdatedAt());
    }
}
