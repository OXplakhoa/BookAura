package com.bookaura.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneOtpRequest(
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9\\s\\-.]{8,17}$", message = "Phone must be 8-15 digits, optional leading +")
        String phone) {
}
