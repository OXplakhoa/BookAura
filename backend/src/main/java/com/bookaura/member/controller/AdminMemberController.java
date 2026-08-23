package com.bookaura.member.controller;

import com.bookaura.auth.entity.AccountStatus;
import com.bookaura.common.web.PageResponse;
import com.bookaura.member.dto.*;
import com.bookaura.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin members", description = "ADMIN member creation, profile management and multi-condition search")
@RestController
@RequestMapping("/api/admin/members")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController {

    private final MemberService memberService;

    public AdminMemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @Operation(summary = "Create USER member (ADMIN)",
            description = "emailVerified defaults false; initialPassword is never returned or logged.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberCreateRequest request) {
        return memberService.create(request);
    }

    @Operation(summary = "Read member detail (ADMIN)")
    @GetMapping("/{id}")
    public MemberResponse detail(@PathVariable UUID id) {
        return memberService.get(id);
    }

    @Operation(summary = "Update allowed profile/status fields (ADMIN)",
            description = "Email is intentionally excluded; registered-email changes require verification.")
    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable UUID id, @Valid @RequestBody MemberUpdateRequest request) {
        return memberService.update(id, request);
    }

    @Operation(summary = "Disable member without deleting loan history (ADMIN)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable UUID id) {
        memberService.disable(id);
    }

    @Operation(summary = "Search members with composable conditions (ADMIN)",
            description = "Date input is strict yyyy/MM/d. Filters: name LIKE, email/phone, DoB range, " +
                    "borrowed book title, status, role, verified email. Sort format: fullName:asc,email:desc.")
    @GetMapping
    public PageResponse<MemberResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String emailOrPhone,
            @RequestParam(required = false) String dateOfBirthFrom,
            @RequestParam(required = false) String dateOfBirthTo,
            @RequestParam(required = false) String borrowedBookTitle,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        return memberService.search(new MemberSearchInput(
                name, emailOrPhone, dateOfBirthFrom, dateOfBirthTo,
                borrowedBookTitle, status, role, emailVerified), page, size, sort);
    }
}
