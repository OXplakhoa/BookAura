package com.bookaura.loan.repository;

import com.bookaura.loan.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    boolean existsByMemberProfile_IdAndBook_IdAndReturnedAtIsNull(UUID memberId, UUID bookId);

    long countByBook_IdAndReturnedAtIsNull(UUID bookId);

    @EntityGraph(attributePaths = {"book", "memberProfile", "memberProfile.userAccount"})
    @Query("SELECT l FROM Loan l WHERE l.id = :id")
    Optional<Loan> findDetailedById(@Param("id") UUID id);

    /** Atomic return guard: only the first request can transition NULL -> returnedAt. */
    @Modifying
    @Query("UPDATE Loan l SET l.returnedAt = :returnedAt WHERE l.id = :loanId AND l.returnedAt IS NULL")
    int markReturnedIfActive(@Param("loanId") UUID loanId, @Param("returnedAt") Instant returnedAt);

    @EntityGraph(attributePaths = {"book", "memberProfile", "memberProfile.userAccount"})
    Page<Loan> findByMemberProfile_IdAndReturnedAtIsNull(UUID memberId, Pageable pageable);

    @EntityGraph(attributePaths = {"book", "memberProfile", "memberProfile.userAccount"})
    Page<Loan> findByMemberProfile_IdAndReturnedAtIsNotNull(UUID memberId, Pageable pageable);

    @EntityGraph(attributePaths = {"book", "memberProfile", "memberProfile.userAccount"})
    Page<Loan> findByReturnedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"book", "memberProfile", "memberProfile.userAccount"})
    Page<Loan> findByReturnedAtIsNotNull(Pageable pageable);

    @EntityGraph(attributePaths = {"book", "memberProfile", "memberProfile.userAccount"})
    @Query(value = "SELECT l FROM Loan l", countQuery = "SELECT COUNT(l) FROM Loan l")
    Page<Loan> findAllDetailed(Pageable pageable);
}
