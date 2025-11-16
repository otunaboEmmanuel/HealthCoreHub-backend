package com.hc.hospitalservice.filter;

import com.hc.hospitalservice.config.TenantContext;
import com.hc.hospitalservice.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1️⃣ Get token from cookie
            Optional<String> tokenOpt = getAccessTokenFromCookie(request);

            if (tokenOpt.isPresent()) {
                String token = tokenOpt.get();

                // 2️⃣ Validate token
                Claims claims = jwtService.extractClaims(token);

                if (jwtService.isTokenValid(token)) {
                    // 3️⃣ Extract tenant DB & store tenant context
                    String tenantDb = claims.get("tenant_db", String.class);
                    TenantContext.setCurrentTenant(tenantDb);

                    // 4️⃣ Attach user details to request for controllers
                    request.setAttribute("userId", claims.get("user_id"));
                    request.setAttribute("email", claims.get("email"));
                    request.setAttribute("tenantRole", claims.get("tenant_role"));
                    request.setAttribute("globalRole", claims.get("global_role"));
                    request.setAttribute("tenantUserId", claims.get("tenant_user_id"));
                    request.setAttribute("tenantDb", tenantDb);

                    log.debug("Tenant context set: {}", tenantDb);
                }
            }

            // Continue request
            filterChain.doFilter(request, response);

        } finally {
            // 5️⃣ Always clear tenant context at end of request
            TenantContext.clear();
        }
    }

    private Optional<String> getAccessTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();

        return Arrays.stream(request.getCookies())
                .filter(c -> "access_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
