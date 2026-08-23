package com.bookaura.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneOtpConfirmRequest(
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9\\s\\-.]{8,17}$", message = "Phone must be 8-15 digits, optional leading +")
        String phone,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Code must contain exactly 6 digits")
        String code) {
}
