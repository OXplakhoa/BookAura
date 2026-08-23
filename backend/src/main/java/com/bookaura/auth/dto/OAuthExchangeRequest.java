package com.bookaura.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OAuthExchangeRequest(@NotBlank @Size(max = 200) String code) {
}
