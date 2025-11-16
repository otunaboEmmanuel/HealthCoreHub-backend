package com.hc.authservice.service;

import com.hc.authservice.dto.LoginRequest;
import com.hc.authservice.dto.LoginResponse;
import com.hc.authservice.dto.RegisterRequest;
import com.hc.authservice.dto.UserInfo;
import com.hc.authservice.entity.AuthUser;
import com.hc.authservice.entity.RefreshToken;
import com.hc.authservice.repository.AuthUserRepository;
import com.hc.authservice.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletResponse;
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
import java.util.UUID;

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
    private final CookieService cookieService;

    /**
     * Register a new user (called by onboarding-service or hospital-service)
     */
    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        log.info(" Registering user: {}", request.getEmail());

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
        log.info(" User registered in auth service: {}", savedUser.getId());

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
    public Map<String,Object> login(LoginRequest request, HttpServletResponse response) {
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

        UserInfo userInfo = getTenantRoleAndId(authUser);
        if (userInfo == null) {
            throw new RuntimeException("User not found in tenant DB: " + authUser.getEmail());
        }

        if (userInfo.getRole() == null) {
            throw new RuntimeException("User has no assigned role in tenant DB: " + authUser.getEmail());
        }

        // Generate tokens
        String accessToken = jwtService.generateToken(authUser, userInfo.getRole(), tenantStatus, userInfo.getUserId());
        String refreshToken = jwtService.generateRefreshToken(authUser);

        // Save refresh token
        saveRefreshToken(authUser, refreshToken);
        // Set cookies
        cookieService.setAccessTokenCookie(response, accessToken);
        cookieService.setRefreshTokenCookie(response, refreshToken);

        log.info(" Login successful for: {}", request.getEmail());
        Map<String,Object> responseMap = new HashMap<>();
        responseMap.put("userId", authUser.getId());
        responseMap.put("email", authUser.getEmail());
        log.info("response is {}", responseMap);
        return responseMap;

    }
    public void logout(HttpServletResponse response) {
        cookieService.clearAuthCookies(response);
        log.info(" User logged out - cookies cleared");
    }

    /**
     * Get user's role from their tenant database
     */
    private UserInfo getTenantRoleAndId(AuthUser authUser) {
        if (authUser.getTenantDb() == null) {
            log.info("User has no tenant DB (likely super admin)");
            return null;
        }

        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, authUser.getTenantDb());


        log.info("Fetching tenant role from: {}", tenantUrl);

        String sql = "SELECT role, id FROM users WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, authUser.getEmail());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
               return UserInfo.builder()
                       .role(rs.getString("role"))
                       .userId(rs.getInt("id"))
                       .build();
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
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration))
                .createdAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(refreshToken);
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
            throw new RuntimeException("No tenant status found for user: " + authUser.getEmail());
        } catch (SQLException e) {
            log.error("Error fetching tenant user status", e);
        }return null;
    }
    //grpc request to register admin for onboarding service
    public AuthUser registerUser(String email, String password, int hospitalId, String tenantDb, String globalRole) {
        log.info("register user from grpc server start with email {}", email);
        if(authUserRepository.existsByEmail(email)) {
            log.info("user already exists with email {}", email);
            throw new IllegalArgumentException("user already exists");
        }
        AuthUser authUser = AuthUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .hospitalId(hospitalId)
                .tenantDb(tenantDb)
                .globalRole(globalRole)
                .status("ACTIVE")
                .emailVerified(true)
                .isActive(true)
                .isLocked(false)
                .failedLoginAttempts(0)
                .emailVerified(true)
                .passwordChangedAt(LocalDateTime.now())
                .build();
        log.info("register user start with email {}", email);
        authUserRepository.save(authUser);
        return authUser;
    }
    //grpc request to register users from onboarding service
    public AuthUser registerStaff(String email, int hospitalId, String tenantDb, String globalRole) {
        log.info("register staff from grpc server start with email {}", email);
        if(authUserRepository.existsByEmail(email)) {
            log.info("user already exists with email {}", email);
            throw new IllegalArgumentException("user already exists");
        }
        AuthUser authUser = AuthUser.builder()
                .email(email)
                .activationToken(UUID.randomUUID().toString())
                .tokenExpired(LocalDateTime.now().plusHours(24))
                .passwordHash("")
                .hospitalId(hospitalId)
                .globalRole(globalRole)
                .status("PENDING")
                .isActive(true)
                .isLocked(false)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .tenantDb(tenantDb)
                .build();
        log.info("register staff start with email {}", email);
        authUserRepository.save(authUser);
        return authUser;
    }
    //grpc request to delete user upon failed instances in hospital service
    public void deleteUser(String userId) {
        log.info("checking if user with id {} exists", userId);
        if(!(authUserRepository.existsById(userId))) {
            log.info("user doesn't exists with id {}", userId);
            throw new IllegalArgumentException("user doesn't exists");
        }
        log.info("deleting user with id {}", userId);
        authUserRepository.deleteById(userId);
    }
    //creating refresh token after access token has expired
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refreshToken(String refreshToken, HttpServletResponse response) {
        log.info(" Token refresh attempt");
        Map<String,Object> responseMap = new HashMap<>();
        //validate refresh token
        RefreshToken refreshToken1 = refreshTokenRepository.findByToken(refreshToken).orElse(null);
        if (refreshToken1 == null) {
            log.info("refresh token not found");
            responseMap.put("error", "Invalid refresh token");
            return responseMap;
        }
        String userId = jwtService.extractUserId(refreshToken);
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("Account is not active");
        }
        UserInfo userInfo = getTenantRoleAndId(user);
        if (userInfo == null) {
            throw new RuntimeException("User not found in tenant DB: " + user.getEmail());
        }
        //get tenant status from tenantDb
        String tenantStatus = getTenantUserStatus(user);
        // Generate new access token
        String newAccessToken = jwtService.generateToken(user, userInfo.getRole(), tenantStatus, userInfo.getUserId());
        // Set new access token cookie (keep refresh token)
        cookieService.setAccessTokenCookie(response, newAccessToken);
        log.info(" Token refreshed for user: {}", user.getEmail());
        responseMap.put("userId", user.getId());
        responseMap.put("success", true);
        responseMap.put("email", user.getEmail());
        return responseMap;
    }
}