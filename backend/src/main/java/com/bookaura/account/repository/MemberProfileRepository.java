package com.bookaura.account.repository;

import com.bookaura.account.entity.MemberProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, UUID> {

    @EntityGraph(attributePaths = "userAccount")
    Optional<MemberProfile> findByUserAccount_Id(UUID userAccountId);
}
