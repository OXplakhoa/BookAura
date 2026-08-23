package com.bookaura.account.repository;

import com.bookaura.account.entity.MemberProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, UUID>, JpaSpecificationExecutor<MemberProfile> {

    @EntityGraph(attributePaths = {"userAccount", "userAccount.roles"})
    Optional<MemberProfile> findByUserAccount_Id(UUID userAccountId);

    @EntityGraph(attributePaths = {"userAccount", "userAccount.roles"})
    @Query("SELECT m FROM MemberProfile m WHERE m.id = :id")
    Optional<MemberProfile> findDetailedById(@Param("id") UUID id);
}
