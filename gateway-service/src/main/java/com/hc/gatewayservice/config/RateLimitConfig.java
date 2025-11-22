package com.hc.gatewayservice.config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class RateLimitConfig {

    /**
     * IP-based rate limiting (fallback)
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Forwarded-For");

            if (ip == null || ip.isEmpty()) {
                ip = exchange.getRequest()
                        .getRemoteAddress()
                        .getAddress()
                        .getHostAddress();
            } else {
                ip = ip.split(",")[0].trim();
            }

            log.debug(" IP Key Resolver: {}", ip);
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * User-based rate limiting (primary - uses JWT user ID from headers)
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Get user ID from header (set by JWT filter)
            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Id");

            if (userId != null && !userId.isEmpty()) {
                log.debug(" User Key Resolver: user:{}", userId);
                return Mono.just("user:" + userId);
            }

            // Fallback to IP if not authenticated
            String ip = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Forwarded-For");

            if (ip == null || ip.isEmpty()) {
                ip = exchange.getRequest()
                        .getRemoteAddress()
                        .getAddress()
                        .getHostAddress();
            } else {
                ip = ip.split(",")[0].trim();
            }

            log.debug(" User Key Resolver (fallback to IP): {}", ip);
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * Tenant-based rate limiting
     */
    @Bean
    public KeyResolver tenantKeyResolver() {
        return exchange -> {
            String tenantDb = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Tenant-Db");

            if (tenantDb != null && !tenantDb.isEmpty()) {
                log.debug(" Tenant Key Resolver: tenant:{}", tenantDb);
                return Mono.just("tenant:" + tenantDb);
            }

            log.debug(" Tenant Key Resolver (default)");
            return Mono.just("tenant:default");
        };
    }
}