package com.hc.authservice.service;

import com.hc.authservice.dto.LoginRequest;
import com.hc.authservice.dto.LoginResponse;
import com.hc.authservice.dto.RegisterRequest;
import com.hc.authservice.entity.AuthUser;
import com.hc.authservice.entity.RefreshToken;
import com.hc.authservice.repository.AuthUserRepository;
import com.hc.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${tenant.datasource.host}")
    private String tenantDbHost;

    @Value("${tenant.datasource.port}")
    private String tenantDbPort;

    @Value("${tenant.datasource.username}")
    private String tenantDbUsername;

    @Value("${tenant.datasource.password}")
    private String tenantDbPassword;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * Register a new user (called by onboarding-service or hospital-service)
     */
    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        log.info("📝 Registering user: {}", request.getEmail());

        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with email '" + request.getEmail() + "' already exists");
        }

        // Create auth user
        AuthUser authUser = AuthUser.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .hospitalId(request.getHospitalId())
                .tenantDb(request.getTenantDb())
                .globalRole(request.getGlobalRole())
                .isActive(true)
                .emailVerified(true)
                .passwordChangedAt(LocalDateTime.now())
                .build();

        AuthUser savedUser = authUserRepository.save(authUser);
        log.info("✅ User registered in auth service: {}", savedUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("userId", savedUser.getId().toString());
        response.put("email", savedUser.getEmail());
        response.put("message", "User registered successfully");

        return response;
    }

    /**
     * User login
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("🔐 Login attempt for: {}", request.getEmail());

        // Find user
        AuthUser authUser = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        String tenantStatus = getTenantUserStatus(authUser);

        if ("PENDING".equals(tenantStatus)) {
            throw new IllegalArgumentException("Account is pending approval. Please contact your hospital.");
        }

        if ("REJECTED".equals(tenantStatus)) {
            throw new IllegalArgumentException("Account has been rejected. Please contact your hospital.");
        }

        // Check if account is locked
        if (Boolean.TRUE.equals(authUser.getIsLocked())) {
            throw new IllegalArgumentException("Account is locked. Please contact support.");
        }

        // Check if account is active
        if (!Boolean.TRUE.equals(authUser.getIsActive())) {
            throw new IllegalArgumentException("Account is inactive. Please contact support.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), authUser.getPasswordHash())) {
            // Increment failed attempts
            authUser.setFailedLoginAttempts(authUser.getFailedLoginAttempts() + 1);

            // Lock account after 5 failed attempts
            if (authUser.getFailedLoginAttempts() >= 5) {
                authUser.setIsLocked(true);
                authUserRepository.save(authUser);
                throw new IllegalArgumentException("Account locked due to multiple failed login attempts");
            }

            authUserRepository.save(authUser);
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Reset failed login attempts on successful login
        authUser.setFailedLoginAttempts(0);
        authUser.setLastLogin(LocalDateTime.now());
        authUserRepository.save(authUser);

        // Get tenant role (if user belongs to a hospital)
        String tenantRole = getTenantRole(authUser);

        // Generate tokens
        String accessToken = jwtService.generateToken(authUser, tenantRole);
        String refreshToken = jwtService.generateRefreshToken(authUser);

        // Save refresh token
        saveRefreshToken(authUser, refreshToken);

        log.info("✅ Login successful for: {}", request.getEmail());

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Get user's role from their tenant database
     */
    private String getTenantRole(AuthUser authUser) {
        if (authUser.getTenantDb() == null) {
            log.info("User has no tenant DB (likely super admin)");
            return null;
        }

        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, authUser.getTenantDb());

        log.info("Fetching tenant role from: {}", tenantUrl);

        String sql = "SELECT role FROM users WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, authUser.getEmail());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                log.info("Found tenant role: {}", role);
                return role;
            }

            log.warn("No tenant role found for user: {}", authUser.getEmail());
            return null;

        } catch (SQLException e) {
            log.error("Error fetching tenant role", e);
            throw new RuntimeException("Failed to fetch tenant role: " + e.getMessage());
        }
    }

    /**
     * Save refresh token
     */
    private void saveRefreshToken(AuthUser user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration))
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Validate token
     */
    public Map<String, Object> validateToken(String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid token");
        }

        var claims = jwtService.extractClaims(token);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("email", claims.getSubject());
        response.put("user_id", claims.get("user_id"));
        response.put("hospital_id", claims.get("hospital_id"));
        response.put("tenant_db", claims.get("tenant_db"));
        response.put("global_role", claims.get("global_role"));
        response.put("tenant_role", claims.get("tenant_role"));

        return response;
    }
    private String getTenantUserStatus(AuthUser authUser) {
        if (authUser.getTenantDb() == null) {
            return null;
        }

        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, authUser.getTenantDb());

        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword)) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT status FROM users WHERE email = ?"
            );
            stmt.setString(1, authUser.getEmail());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("status");
            }
        } catch (SQLException e) {
            log.error("Error fetching tenant user status", e);
        }return null;
    }
}