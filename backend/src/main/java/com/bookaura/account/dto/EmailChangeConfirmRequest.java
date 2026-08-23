package com.bookaura.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailChangeConfirmRequest(
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Code must contain exactly 6 digits") String code) {
}
