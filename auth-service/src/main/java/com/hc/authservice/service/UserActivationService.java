package com.hc.authservice.service;

import com.hc.authservice.entity.AuthUser;
import com.hc.authservice.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserActivationService {
    @Value("${tenant.datasource.host}")
    private String tenantDbHost;

    @Value("${tenant.datasource.port}")
    private String tenantDbPort;
    @Value("${tenant.datasource.username}")
    private String tenantDbUsername;

    @Value("${tenant.datasource.password}")
    private String tenantDbPassword;
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> validateToken(String token) {
        Map<String, Object> response = new HashMap<>();
       AuthUser authUser = authUserRepository.findByActivationToken(token).orElse(null);
       if (authUser == null) {
           log.warn("Invalid activation Token {}", token);
           response.put("error", "Token does not exist");
           return response;
       }
        if (validateToken(response, authUser)) return response;
        response.put("success", true);
       response.put("message", "Activation Token has been validated successfully");
       return response;
    }

    public Map<String, Object> activateUser(String password, String token) {
        log.info("Activating user {}", token);
        Map<String, Object> response = new HashMap<>();
        AuthUser authUser = authUserRepository.findByActivationToken(token).orElse(null);
        if (authUser == null) {
            log.warn("Invalid activation Token {}", token);
            response.put("error", "Invalid activation Token");
            return response;
        }
        if (validateToken(response, authUser)) return response;
        validatePassword(password);
        authUser.setPasswordHash(passwordEncoder.encode(password));
        authUser.setIsActive(true);
        authUser.setActivationToken(null);
        authUser.setTokenExpired(null);
        authUser.setStatus("ACTIVE");
        activateUserInTenantDb(authUser);
        authUserRepository.save(authUser);
        log.info("Activated user {}", authUser);
        response.put("success", true);
        response.put("message", "User has been activated successfully");
        //figure out how to implement cookies and return for this user
        return response;
    }

    private boolean validateToken(Map<String, Object> response, AuthUser authUser) {
        if(authUser.getTokenExpired().isBefore(LocalDateTime.now())) {
            log.warn("Token has expired");
            response.put("error", "Token has expired");
            return true;
        }
        if(!(authUser.getStatus().equals("PENDING"))){
            log.info("user is already created status is not pending. Status: {}", authUser.getStatus());
            response.put("error", "User is already created status is not pending");
            return true;
        }
        return false;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }

        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }
    private void activateUserInTenantDb(AuthUser authUser) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s:/%s",tenantDbHost, tenantDbPort, authUser.getTenantDb());
        String sql = """
                INSERT INTO users(password, updated_at) VALUES (?, CURRENT_TIMESTAMP)
                """;
        try (Connection connection = DriverManager.getConnection(tenantUrl, tenantDbUsername,tenantDbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1,authUser.getPasswordHash());
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                log.warn("Query failed for activation for user {}", authUser.getId());
                throw new RuntimeException("Query failed for activation for user " + authUser.getId());
            }
            log.info("Inserted password for user {}", authUser.getId());
        }catch (SQLException e){
            log.error("Error while inserting password for user {}", authUser.getId(), e);
            throw new RuntimeException(e);
        }
    }
}
