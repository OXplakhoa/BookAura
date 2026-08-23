package com.bookaura.loan.service;

import com.bookaura.account.entity.MemberProfile;
import com.bookaura.account.repository.MemberProfileRepository;
import com.bookaura.catalog.entity.Book;
import com.bookaura.catalog.repository.BookRepository;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.common.logging.LogOperation;
import com.bookaura.common.web.PageResponse;
import com.bookaura.loan.dto.LoanResponse;
import com.bookaura.loan.entity.Loan;
import com.bookaura.loan.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Borrow/return transaction boundary. Concurrency strategy is deliberately ONE concept:
 * conditional atomic UPDATE + affected-row count. No pessimistic/optimistic lock combination.
 */
@Service
@LogOperation
public class LoanService {

    private static final Duration DEFAULT_LOAN_PERIOD = Duration.ofDays(14);
    private static final Logger AUDIT = LoggerFactory.getLogger("com.bookaura.audit");

    private final MemberProfileRepository memberRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final LoanPageRequestFactory pageRequestFactory;
    private final LoanMapper mapper;
    private final ObjectProvider<LoanFailureHook> failureHooks;

    public LoanService(MemberProfileRepository memberRepository, BookRepository bookRepository,
                       LoanRepository loanRepository, LoanPageRequestFactory pageRequestFactory,
                       LoanMapper mapper, ObjectProvider<LoanFailureHook> failureHooks) {
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.pageRequestFactory = pageRequestFactory;
        this.mapper = mapper;
        this.failureHooks = failureHooks;
    }

    @Transactional
    public LoanResponse borrow(UUID userAccountId, UUID bookId) {
        MemberProfile member = memberForUser(userAccountId);
        Book book = bookRepository.findActiveDetailedById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND, "Active book not found: " + bookId));

        if (loanRepository.existsByMemberProfile_IdAndBook_IdAndReturnedAtIsNull(member.getId(), bookId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ACTIVE_LOAN,
                    "You already have an active loan for this book");
        }
        int obtained = bookRepository.decrementAvailableIfPossible(bookId);
        if (obtained != 1) {
            throw new BusinessException(ErrorCode.BOOK_OUT_OF_STOCK, "Book has no available inventory");
        }
        runFailureHooks(LoanMutation.BORROW); // test-only when a bean is present

        Instant now = Instant.now();
        Loan loan = new Loan();
        loan.setMemberProfile(member);
        loan.setBook(book);
        loan.setBorrowedAt(now);
        loan.setDueAt(now.plus(DEFAULT_LOAN_PERIOD));
        try {
            loanRepository.saveAndFlush(loan);
        } catch (DataIntegrityViolationException ex) {
            // PostgreSQL partial unique index closes the concurrent same-member/same-book race.
            throw new BusinessException(ErrorCode.DUPLICATE_ACTIVE_LOAN,
                    "You already have an active loan for this book");
        }
        AUDIT.info("event=BOOK_BORROWED loanId={} memberId={} bookId={}", loan.getId(), member.getId(), bookId);
        return mapper.toResponse(loan);
    }

    @Transactional
    public LoanResponse returnOwn(UUID userAccountId, UUID loanId) {
        return doReturn(userAccountId, loanId, false);
    }

    @Transactional
    public LoanResponse returnAsAdmin(UUID adminUserId, UUID loanId) {
        return doReturn(adminUserId, loanId, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> activeForUser(UUID userAccountId, int page, int size, String sort) {
        MemberProfile member = memberForUser(userAccountId);
        Page<Loan> result = loanRepository.findByMemberProfile_IdAndReturnedAtIsNull(
                member.getId(), pageRequestFactory.create(page, size, sort));
        return PageResponse.from(result, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> historyForUser(UUID userAccountId, int page, int size, String sort) {
        MemberProfile member = memberForUser(userAccountId);
        Page<Loan> result = loanRepository.findByMemberProfile_IdAndReturnedAtIsNotNull(
                member.getId(), pageRequestFactory.create(page, size, sort));
        return PageResponse.from(result, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> listAdmin(Boolean active, int page, int size, String sort) {
        var pageable = pageRequestFactory.create(page, size, sort);
        Page<Loan> result = active == null
                ? loanRepository.findAllDetailed(pageable)
                : active ? loanRepository.findByReturnedAtIsNull(pageable)
                : loanRepository.findByReturnedAtIsNotNull(pageable);
        return PageResponse.from(result, mapper::toResponse);
    }

    private LoanResponse doReturn(UUID actorUserId, UUID loanId, boolean adminOverride) {
        Loan loan = loanRepository.findDetailedById(loanId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND, "Loan not found: " + loanId));
        UUID ownerId = loan.getMemberProfile().getUserAccount().getId();
        if (!adminOverride && !ownerId.equals(actorUserId)) {
            throw new BusinessException(ErrorCode.LOAN_NOT_OWNED, "You cannot return another member's loan");
        }
        if (loan.getReturnedAt() != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_RETURN, "Loan has already been returned");
        }

        Instant returnedAt = Instant.now();
        if (loanRepository.markReturnedIfActive(loanId, returnedAt) != 1) {
            throw new BusinessException(ErrorCode.DUPLICATE_RETURN, "Loan has already been returned");
        }
        if (bookRepository.incrementAvailableIfBelowTotal(loan.getBook().getId()) != 1) {
            throw new BusinessException(ErrorCode.INVENTORY_INCONSISTENT,
                    "Inventory invariant rejected this return; transaction rolled back");
        }
        runFailureHooks(LoanMutation.RETURN);

        // JPQL bulk updates do not synchronize already-managed entities automatically.
        // Keep this managed instance consistent for response mapping; DB has the same value.
        loan.setReturnedAt(returnedAt);
        AUDIT.info("event=BOOK_RETURNED loanId={} memberId={} bookId={} adminOverride={}",
                loanId, loan.getMemberProfile().getId(), loan.getBook().getId(), adminOverride);
        return mapper.toResponse(loan);
    }

    private MemberProfile memberForUser(UUID userAccountId) {
        return memberRepository.findByUserAccount_Id(userAccountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_PROFILE_NOT_FOUND,
                        "Member profile not found for account: " + userAccountId));
    }

    private void runFailureHooks(LoanMutation mutation) {
        failureHooks.orderedStream().forEach(hook -> hook.afterMutation(mutation));
    }
}
