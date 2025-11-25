package com.hc.gatewayservice.filter;

import com.hc.gatewayservice.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/oauth",
            "/api/plans"  // Public endpoints add more from hospital service
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Skip JWT validation for public endpoints
        if (isPublicPath(path)) {
            log.debug(" Public path, skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }

        // Extract JWT from cookie
        HttpCookie accessTokenCookie = exchange.getRequest()
                .getCookies()
                .getFirst("access_token");

        if (accessTokenCookie == null) {
            log.warn(" No access token cookie found for: {}", path);
            return handleUnauthorized(exchange);
        }

        String token = accessTokenCookie.getValue();

        try {
            // Validate JWT and extract claims
            Claims claims = jwtService.validateAndExtractClaims(token);

            // Extract user context from JWT
            String userId = jwtService.extractClaim(claims, "user_id");
            String email = jwtService.extractClaim(claims, "email");
            String hospitalId = jwtService.extractClaim(claims, "hospital_id");
            String tenantDb = jwtService.extractClaim(claims, "tenant_db");
            String globalRole = jwtService.extractClaim(claims, "global_role");
            String tenantRole = jwtService.extractClaim(claims, "tenant_role");
            String tenantUserId = jwtService.extractClaim(claims, "tenant_user_id");
            String status = jwtService.extractClaim(claims, "status");
            String firstName = jwtService.extractClaim(claims, "first_name");
            String lastName = jwtService.extractClaim(claims, "last_name");

            // Build modified request with user context headers
            ServerHttpRequest modifiedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", userId)
                    .header("X-Email", email)
                    .header("X-Hospital-Id", hospitalId)
                    .header("X-Tenant-Db", tenantDb)
                    .header("X-Global-Role", globalRole)
                    .header("X-Tenant-Role", tenantRole)
                    .header("X-Tenant-User-Id", tenantUserId)
                    .header("X-User-Status", status)
                    .header("X-Tenant-FirstName", firstName)
                    .header("X-Tenant-LastName", lastName)
                    .build();

            log.info(" JWT validated for user: {} | Role: {} | Path: {}",
                    email, globalRole, path);

            // Continue with modified request
            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.error(" JWT validation failed for path: {} | Error: {}", path, e.getMessage());
            return handleUnauthorized(exchange);
        }
    }

    /**
     * Check if path is public (no auth required)
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Handle unauthorized requests
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String body = """
            {
                "error": "unauthorized",
                "message": "Authentication required"
            }
            """;

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100; // Run before rate limiting
    }
}
