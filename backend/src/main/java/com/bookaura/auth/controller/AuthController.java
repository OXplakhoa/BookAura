package com.bookaura.auth.controller;

import com.bookaura.auth.dto.*;
import com.bookaura.auth.oauth.OAuthLoginService;
import com.bookaura.auth.oauth.OAuthProviderAvailability;
import com.bookaura.auth.service.AuthService;
import com.bookaura.auth.sms.PhoneOtpService;
import com.bookaura.auth.token.RefreshCookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Tag(name = "Auth", description = "Registration, verification, login, token lifecycle")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieService cookieService;
    private final OAuthLoginService oauthLoginService;
    private final OAuthProviderAvailability oauthProviders;
    private final PhoneOtpService phoneOtpService;

    public AuthController(AuthService authService, RefreshCookieService cookieService,
                          OAuthLoginService oauthLoginService, OAuthProviderAvailability oauthProviders,
                          PhoneOtpService phoneOtpService) {
        this.authService = authService;
        this.cookieService = cookieService;
        this.oauthLoginService = oauthLoginService;
        this.oauthProviders = oauthProviders;
        this.phoneOtpService = phoneOtpService;
    }

    @Operation(summary = "Register with email + password; sends verification email")
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Registration successful. Please verify your email before logging in."));
    }

    @Operation(summary = "Verify email with the token from the verification link")
    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
        return new MessageResponse("Email verified. You can now log in.");
    }

    @Operation(summary = "Resend verification email (enumeration-safe)")
    @PostMapping("/resend-verification")
    public MessageResponse resend(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.email());
        return new MessageResponse("If the account exists and is unverified, a new verification email was sent.");
    }

    @Operation(summary = "Login with email or phone + password. Sets refresh cookie; returns access token.")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest http, HttpServletResponse response) {
        return toAuthResponse(authService.login(request, http), response);
    }

    @Operation(summary = "List configured OAuth providers without exposing client credentials")
    @GetMapping("/oauth/providers")
    public OAuthProvidersResponse oauthProviders() {
        return new OAuthProvidersResponse(oauthProviders.isGoogleConfigured());
    }

    @Operation(summary = "Exchange a 60-second single-use OAuth redirect code for the normal app session")
    @PostMapping("/oauth/exchange")
    public AuthResponse exchangeOAuth(@Valid @RequestBody OAuthExchangeRequest request,
                                      HttpServletRequest http, HttpServletResponse response) {
        return toAuthResponse(oauthLoginService.exchange(request.code(), http), response);
    }

    @Operation(summary = "Request an enumeration-safe five-minute phone login code")
    @PostMapping("/phone-otp/request")
    public MessageResponse requestPhoneOtp(@Valid @RequestBody PhoneOtpRequest request) {
        phoneOtpService.request(request.phone());
        return new MessageResponse("If an active account uses this phone, a code was sent. Please wait before retrying.");
    }

    @Operation(summary = "Consume a phone login code and issue the normal app session")
    @PostMapping("/phone-otp/confirm")
    public AuthResponse confirmPhoneOtp(@Valid @RequestBody PhoneOtpConfirmRequest request,
                                        HttpServletRequest http, HttpServletResponse response) {
        return toAuthResponse(phoneOtpService.confirm(request.phone(), request.code(), http), response);
    }

    @Operation(summary = "Rotate refresh token (HttpOnly cookie) -> new access token + new cookie")
    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest http, HttpServletResponse response) {
        String raw = cookieService.readCookie(http).orElse(null);
        return toAuthResponse(authService.refresh(raw, http), response);
    }

    @Operation(summary = "Logout: revoke refresh session + blacklist current access token jti")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
                                       HttpServletRequest http, HttpServletResponse response) {
        authService.logout(auth, cookieService.readCookie(http).orElse(null));
        cookieService.clearCookie(response);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Current authenticated user")
    @GetMapping("/me")
    public AuthResponse.UserSummary me(Authentication authentication) {
        return authService.me(UUID.fromString(authentication.getName()));
    }

    private AuthResponse toAuthResponse(AuthService.LoginResult result, HttpServletResponse response) {
        cookieService.setCookie(response, result.rawRefreshToken(),
                Duration.between(Instant.now(), result.refreshExpiresAt()));
        return new AuthResponse(result.access().token(), "Bearer", result.access().expiresInSeconds(),
                result.user());
    }
}
