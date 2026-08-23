package com.bookaura.loan.controller;

import com.bookaura.common.web.PageResponse;
import com.bookaura.loan.dto.BorrowRequest;
import com.bookaura.loan.dto.LoanResponse;
import com.bookaura.loan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "My loans", description = "Authenticated USER borrow/return/history flows")
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @Operation(summary = "Borrow one available copy (atomic inventory decrement)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse borrow(@Valid @RequestBody BorrowRequest request, Authentication authentication) {
        return loanService.borrow(userId(authentication), request.bookId());
    }

    @Operation(summary = "Return own active loan; duplicate return is rejected")
    @PostMapping("/{loanId}/return")
    public LoanResponse returnBook(@PathVariable UUID loanId, Authentication authentication) {
        return loanService.returnOwn(userId(authentication), loanId);
    }

    @Operation(summary = "List my active loans (page size max 10)")
    @GetMapping("/active")
    public PageResponse<LoanResponse> active(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return loanService.activeForUser(userId(authentication), page, size, sort);
    }

    @Operation(summary = "List my returned-loan history (page size max 10)")
    @GetMapping("/history")
    public PageResponse<LoanResponse> history(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return loanService.historyForUser(userId(authentication), page, size, sort);
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
