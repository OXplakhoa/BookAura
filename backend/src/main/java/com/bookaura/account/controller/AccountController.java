package com.bookaura.account.controller;

import com.bookaura.account.dto.EmailChangeConfirmRequest;
import com.bookaura.account.dto.EmailChangeRequest;
import com.bookaura.account.dto.EmailChangeResponse;
import com.bookaura.account.service.EmailChangeService;
import com.bookaura.auth.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "My account", description = "Authenticated account security and identity changes")
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final EmailChangeService emailChangeService;

    public AccountController(EmailChangeService emailChangeService) {
        this.emailChangeService = emailChangeService;
    }

    @Operation(summary = "Send a six-digit confirmation code to a new email address")
    @PostMapping("/email-change/request")
    public MessageResponse requestEmailChange(@Valid @RequestBody EmailChangeRequest request,
                                              Authentication authentication) {
        emailChangeService.request(userId(authentication), request.newEmail());
        return new MessageResponse("A confirmation code was sent to the new email address.");
    }

    @Operation(summary = "Consume the latest email-change code and update the registered email")
    @PostMapping("/email-change/confirm")
    public EmailChangeResponse confirmEmailChange(@Valid @RequestBody EmailChangeConfirmRequest request,
                                                  Authentication authentication) {
        return emailChangeService.confirm(userId(authentication), request.code());
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
