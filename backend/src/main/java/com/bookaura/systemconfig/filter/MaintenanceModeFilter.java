package com.bookaura.systemconfig.filter;

import com.bookaura.common.error.ApiError;
import com.bookaura.common.error.ErrorCode;
import com.bookaura.systemconfig.service.SystemConfigurationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Returns 503 for normal APIs while maintenance is ON.
 * Operational exceptions are deliberately narrow:
 * - /api/admin/system-config/** continues into Spring Security + @PreAuthorize (not public)
 * - /actuator/health remains available
 * - OPTIONS passes so browser CORS preflight works; the actual business request still gets 503
 */
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private final SystemConfigurationService configurationService;
    private final ObjectMapper objectMapper;

    public MaintenanceModeFilter(SystemConfigurationService configurationService, ObjectMapper objectMapper) {
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!configurationService.isMaintenanceMode()) return true;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        return path.equals("/actuator/health")
                || path.startsWith("/actuator/health/")
                || path.equals("/api/admin/system-config")
                || path.startsWith("/api/admin/system-config/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, "60");
        ApiError error = ApiError.of(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                ErrorCode.MAINTENANCE_MODE.name(),
                "Service Unavailable",
                "BookAura is temporarily under maintenance",
                request.getRequestURI(),
                MDC.get("traceId"));
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
