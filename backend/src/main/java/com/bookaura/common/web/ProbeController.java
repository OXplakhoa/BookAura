package com.bookaura.common.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Tiny probes to demonstrate the authorization matrix (used by tests and the mentor demo).
 */
@RestController
public class ProbeController {

    @GetMapping("/api/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminPing() {
        return Map.of("status", "admin-ok");
    }
}
