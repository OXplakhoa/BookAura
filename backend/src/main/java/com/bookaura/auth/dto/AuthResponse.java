package com.bookaura.auth.dto;

import java.util.List;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
    public record UserSummary(UUID id, String email, String fullName, List<String> roles) {
    }
}
