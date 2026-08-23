package com.bookaura.systemconfig.dto;

import java.time.Instant;
import java.util.UUID;

public record SystemConfigurationResponse(
        boolean maintenanceMode,
        Instant updatedAt,
        UUID updatedBy
) {
}
