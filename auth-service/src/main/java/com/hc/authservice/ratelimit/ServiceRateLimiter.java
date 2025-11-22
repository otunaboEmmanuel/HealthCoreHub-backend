package com.hc.authservice.ratelimit;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rate-limit.service-level.enabled", havingValue = "true", matchIfMissing = true)
public class ServiceRateLimiter {

    private final RateLimitProperties rateLimitProperties;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    private ProxyManager<String> proxyManager;

    @PostConstruct
    public void init() {
        try {
            RedisClient redisClient = RedisClient.create(
                    String.format("redis://%s:%d", redisHost, redisPort)
            );

            StatefulRedisConnection<String, byte[]> connection =
                    redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

            proxyManager = LettuceBasedProxyManager.builderFor(connection)
                    .withExpirationStrategy(
                            io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
                                    .basedOnTimeForRefillingBucketUpToMax(Duration.ofHours(2))
                    )
                    .build();

            log.info(" Service-level rate limiter initialized with Redis");
        } catch (Exception e) {
            log.error(" Failed to initialize service-level rate limiter", e);
            throw new RuntimeException("Failed to initialize rate limiting", e);
        }
    }

    /**
     * Check rate limit for login attempts (by email)
     */
    public boolean checkLoginRateLimit(String email) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        String key = "auth:login:" + email.toLowerCase();
        RateLimitProperties.EndpointLimit config = rateLimitProperties.getLogin();

        Bucket bucket = proxyManager.builder().build(key, () ->
                BucketConfiguration.builder()
                        .addLimit(Bandwidth.classic(
                                config.getCapacity(),
                                Refill.intervally(
                                        config.getRefillRate(),
                                        Duration.ofMinutes(config.getRefillPeriodMinutes())
                                )
                        ))
                        .build()
        );

        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            long waitSeconds = bucket.estimateAbilityToConsume(1)
                    .getNanosToWaitForRefill() / 1_000_000_000;
            log.warn(" Login rate limit exceeded for email: {} | Wait: {}s",
                    email, waitSeconds);
        } else {
            log.debug(" Login rate limit check passed for: {}", email);
        }

        return allowed;
    }

    /**
     * Check rate limit for OAuth callbacks (by IP)
     */
    public boolean checkOAuthCallbackRateLimit(String ipAddress) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        String key = "auth:oauth:" + ipAddress;
        RateLimitProperties.EndpointLimit config = rateLimitProperties.getOauthCallback();

        Bucket bucket = proxyManager.builder().build(key, () ->
                BucketConfiguration.builder()
                        .addLimit(Bandwidth.classic(
                                config.getCapacity(),
                                Refill.intervally(
                                        config.getRefillRate(),
                                        Duration.ofMinutes(config.getRefillPeriodMinutes())
                                )
                        ))
                        .build()
        );

        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn(" OAuth callback rate limit exceeded for IP: {}", ipAddress);
        }

        return allowed;
    }

    /**
     * Check rate limit for password reset (by email)
     */
    public boolean checkPasswordResetRateLimit(String email) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        String key = "auth:password-reset:" + email.toLowerCase();
        RateLimitProperties.EndpointLimit config = rateLimitProperties.getPasswordReset();

        Bucket bucket = proxyManager.builder().build(key, () ->
                BucketConfiguration.builder()
                        .addLimit(Bandwidth.classic(
                                config.getCapacity(),
                                Refill.intervally(
                                        config.getRefillRate(),
                                        Duration.ofMinutes(config.getRefillPeriodMinutes())
                                )
                        ))
                        .build()
        );

        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn(" Password reset rate limit exceeded for email: {}", email);
        }

        return allowed;
    }

    /**
     * Get seconds until rate limit resets for a specific key
     */
    public long getSecondsUntilReset(String key) {
        Bucket bucket = proxyManager.builder().build(key, () ->
                BucketConfiguration.builder()
                        .addLimit(Bandwidth.classic(1, Refill.intervally(1, Duration.ofMinutes(1))))
                        .build()
        );

        return bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000;
    }
    /**
     * Get remaining attempts for login
     */
    public long getRemainingLoginAttempts(String email) {
        String key = "auth:login:" + email.toLowerCase();
        Bucket bucket = proxyManager.builder().build(key, () ->
                BucketConfiguration.builder()
                        .addLimit(Bandwidth.classic(
                                rateLimitProperties.getLogin().getCapacity(),
                                Refill.intervally(
                                        rateLimitProperties.getLogin().getRefillRate(),
                                        Duration.ofMinutes(rateLimitProperties.getLogin().getRefillPeriodMinutes())
                                )
                        ))
                        .build()
        );

        return bucket.getAvailableTokens();
    }
}