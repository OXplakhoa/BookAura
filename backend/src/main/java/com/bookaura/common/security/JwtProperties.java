package com.bookaura.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bookaura.security")
@Getter
@Setter
public class JwtProperties {

    /** HS256 secret, >= 32 bytes. From JWT_SECRET env outside local/test. */
    private String jwtSecret;

    private int accessTokenTtlMinutes = 15;

    private int refreshTokenTtlDays = 7;

    private String refreshCookieName = "bookaura_refresh";

    private boolean refreshCookieSecure = true;
}
