package com.bookaura.systemconfig.dto;

import jakarta.validation.constraints.NotNull;

public record MaintenanceModeRequest(@NotNull Boolean enabled) {
}
