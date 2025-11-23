package com.hc.hospitalservice.filter;

import com.hc.hospitalservice.config.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract tenant DB from gateway header
            String tenantDb = request.getHeader("X-Tenant-Db");

            if (tenantDb != null && !tenantDb.isEmpty()) {
                TenantContext.setCurrentTenant(tenantDb);
                log.debug(" Tenant context set: {}", tenantDb);
            }

            // Extract and set other user context (optional, for logging/auditing)
            String userId = request.getHeader("X-User-Id");
            String email = request.getHeader("X-Email");
            String globalRole = request.getHeader("X-Global-Role");
            String tenantRole = request.getHeader("X-Tenant-Role");
            String tenantUserId = request.getHeader("X-Tenant-User-Id");

            // Attach to request attributes (for controllers if needed)
            if (userId != null) request.setAttribute("userId", userId);
            if (email != null) request.setAttribute("email", email);
            if (globalRole != null) request.setAttribute("globalRole", globalRole);
            if (tenantRole != null) request.setAttribute("tenantRole", tenantRole);
            if (tenantUserId != null) request.setAttribute("tenantUserId", tenantUserId);
            if (tenantDb != null) request.setAttribute("tenantDb", tenantDb);

            log.debug(" User context | Email: {} | Role: {} | Tenant: {}",
                    email, globalRole, tenantDb);

            filterChain.doFilter(request, response);

        } finally {
            // Always clear tenant context
            TenantContext.clear();
        }
    }
}
