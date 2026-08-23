package com.bookaura.loan.controller;

import com.bookaura.common.web.PageResponse;
import com.bookaura.loan.dto.LoanResponse;
import com.bookaura.loan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin loans", description = "ADMIN-only borrowing activity management")
@RestController
@RequestMapping("/api/admin/loans")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLoanController {

    private final LoanService loanService;

    public AdminLoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @Operation(summary = "List all/active/returned loans (ADMIN)")
    @GetMapping
    public PageResponse<LoanResponse> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return loanService.listAdmin(active, page, size, sort);
    }

    @Operation(summary = "Return a member loan on behalf of the member (ADMIN)")
    @PostMapping("/{loanId}/return")
    public LoanResponse returnBook(@PathVariable UUID loanId, Authentication authentication) {
        return loanService.returnAsAdmin(UUID.fromString(authentication.getName()), loanId);
    }
}
