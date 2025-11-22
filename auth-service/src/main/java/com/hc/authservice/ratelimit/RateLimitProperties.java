package com.hc.authservice.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limit.service-level")
public class RateLimitProperties {

    private boolean enabled = true;

    private EndpointLimit login = new EndpointLimit();
    private EndpointLimit oauthCallback = new EndpointLimit();
    private EndpointLimit passwordReset = new EndpointLimit();

    @Data
    public static class EndpointLimit {
        private int capacity = 10;
        private int refillRate = 10;
        private int refillPeriodMinutes = 1;
    }
}
