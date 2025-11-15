package com.hc.authservice.service;

import com.hc.authservice.entity.AuthUser;
import com.hc.authservice.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserActivationService {
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
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
        authUserRepository.save(authUser);
        log.info("Activated user {}", authUser);

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
        String
    }
}
