package com.bookaura.systemconfig.controller;

import com.bookaura.systemconfig.dto.MaintenanceModeRequest;
import com.bookaura.systemconfig.dto.SystemConfigurationResponse;
import com.bookaura.systemconfig.service.SystemConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "System configuration", description = "ADMIN operational controls; available during maintenance")
@RestController
@RequestMapping("/api/admin/system-config")
@PreAuthorize("hasRole('ADMIN')")
public class SystemConfigurationController {

    private final SystemConfigurationService service;

    public SystemConfigurationController(SystemConfigurationService service) {
        this.service = service;
    }

    @Operation(summary = "Read current system configuration (ADMIN)")
    @GetMapping
    public SystemConfigurationResponse get() {
        return service.get();
    }

    @Operation(summary = "Turn maintenance mode on/off (ADMIN)",
            description = "This protected endpoint and health remain reachable while maintenance is ON.")
    @PutMapping("/maintenance")
    public SystemConfigurationResponse setMaintenance(
            @Valid @RequestBody MaintenanceModeRequest request,
            Authentication authentication) {
        return service.setMaintenanceMode(request.enabled(), UUID.fromString(authentication.getName()));
    }
}
