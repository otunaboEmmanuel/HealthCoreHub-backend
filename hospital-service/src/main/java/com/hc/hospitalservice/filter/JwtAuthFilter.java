package com.hc.hospitalservice.filter;

import com.hc.hospitalservice.config.TenantContext;
import com.hc.hospitalservice.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);

                if (jwtService.isTokenValid(token)) {
                    Claims claims = jwtService.extractClaims(token);
                    String tenantDb = claims.get("tenant_db", String.class);

                    // Store tenant context for this request
                    TenantContext.setCurrentTenant(tenantDb);

                    log.debug("Set tenant context: {}", tenantDb);
                }
            } catch (Exception e) {
                log.warn("Failed to process JWT: {}", e.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always clear context after request
            TenantContext.clear();
        }
    }
}
