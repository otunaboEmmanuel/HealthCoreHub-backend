package com.hc.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProperties {
    private String domain; // .yourdomain.com for cross-subdomain
    private boolean secure = true; // HTTPS only in production
    private boolean httpOnly = true;
    private String sameSite = "None"; // For cross-domain
    private int accessTokenMaxAge = 86400; // 1 day in seconds
    private int refreshTokenMaxAge = 604800; // 7 days in seconds
    private String accessTokenName = "access_token";
    private String refreshTokenName = "refresh_token";
}