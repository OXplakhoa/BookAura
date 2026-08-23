package com.bookaura.auth.repository;

import com.bookaura.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    /** Email is stored normalized lowercase, so plain equality is enough (no LOWER() needed). */
    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, UUID id);

    /** Roles needed to build the Authentication; fetched in one query to avoid N+1/lazy issues in the filter. */
    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findWithRolesById(UUID id);
}
