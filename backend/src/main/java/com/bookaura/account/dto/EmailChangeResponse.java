package com.bookaura.account.dto;

import com.bookaura.auth.dto.AuthResponse;

public record EmailChangeResponse(String message, AuthResponse.UserSummary user) {
}
