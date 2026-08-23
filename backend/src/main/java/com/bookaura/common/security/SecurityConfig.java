package com.bookaura.common.security;

import com.bookaura.auth.oauth.OAuthLoginFailureHandler;
import com.bookaura.auth.oauth.OAuthLoginSuccessHandler;
import com.bookaura.common.error.ApiError;
import com.bookaura.systemconfig.filter.MaintenanceModeFilter;
import com.bookaura.systemconfig.service.SystemConfigurationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless JWT API security.
 * CSRF is disabled because this is a token-based API, not a browser session app; the single
 * cookie we use (refresh token) is protected by SameSite=Lax + a strict CORS allowlist (D4).
 * Method-level authorization (@PreAuthorize) is the authoritative role enforcement.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   SystemConfigurationService systemConfigurationService,
                                                   ObjectProvider<ClientRegistrationRepository> clientRegistrations,
                                                   OAuthLoginSuccessHandler oauthSuccessHandler,
                                                   OAuthLoginFailureHandler oauthFailureHandler)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public catalog: read-only. Backend still protects every mutation.
                        .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/oauth/providers",
                                "/api/auth/oauth/exchange",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) -> {
                            // 401: not authenticated. Specific code when the JWT filter diagnosed the failure.
                            String code = (String) req.getAttribute(JwtAuthenticationFilter.ATTR_JWT_ERROR);
                            if (code == null) {
                                code = "UNAUTHORIZED";
                            }
                            writeError(res, HttpServletResponse.SC_UNAUTHORIZED, code, "Unauthorized", req.getRequestURI());
                        })
                        .accessDeniedHandler((req, res, deniedEx) ->
                                // 403: authenticated but lacks the role/authority.
                                writeError(res, HttpServletResponse.SC_FORBIDDEN, "ACCESS_DENIED",
                                        "You do not have permission to access this resource", req.getRequestURI()))
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // CORS + JWT run first; maintenance blocks before URL/method authorization.
                // The skipped admin-control endpoint still continues to authoritative @PreAuthorize.
                .addFilterAfter(new MaintenanceModeFilter(systemConfigurationService, objectMapper),
                        UsernamePasswordAuthenticationFilter.class);

        // OAuth login endpoints exist only when non-blank Google credentials produced a registration bean.
        // The API remains fully bootable and testable without demo credentials.
        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .successHandler(oauthSuccessHandler)
                    .failureHandler(oauthFailureHandler));
        }
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${bookaura.frontend-url}") String frontendUrl) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl)); // explicit origin, never "*" with credentials
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Trace-Id"));
        config.setExposedHeaders(List.of("X-Trace-Id"));
        config.setAllowCredentials(true); // refresh cookie is cross-origin in local dev (5173 -> 8080)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeError(HttpServletResponse res, int status, String code, String message, String path)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = ApiError.of(status, code,
                status == 401 ? "Unauthorized" : "Forbidden", message, path, MDC.get("traceId"));
        res.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
