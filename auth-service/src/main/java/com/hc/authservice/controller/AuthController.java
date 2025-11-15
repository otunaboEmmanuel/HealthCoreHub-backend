package com.hc.authservice.controller;

import com.hc.authservice.dto.LoginRequest;
import com.hc.authservice.dto.LoginResponse;
import com.hc.authservice.dto.RegisterRequest;
import com.hc.authservice.service.AuthService;
import com.hc.authservice.service.UserActivationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class AuthController {

    private final AuthService authService;
    private final UserActivationService userActivationService;

    /**
     * Register a new user (called by other services)
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        log.info(" Registration request for: {}", request.getEmail());

        try {
            Map<String, Object> response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn(" Registration failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error(" Registration error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Registration failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * User login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info(" Login request for: {}", request.getEmail());

        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn(" Login failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (Exception e) {
            log.error(" Login error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Login failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Validate JWT token
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Map<String, Object> response = authService.validateToken(token);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.warn("⚠️ Token validation failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "auth-service");
        return ResponseEntity.ok(response);
    }
    //validate token from email request
    @PostMapping("validate-token")
    public ResponseEntity<?> validateUserToken(@RequestParam String token) {
        try {
            Map<String, Object> response = userActivationService.validateToken(token);
            return ResponseEntity.ok(response);
        }catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
    @PostMapping("update-password")
    public ResponseEntity<?> activateUser(@RequestBody Map<String, String> request) {
        try{
            Map<String, Object> result = userActivationService.activateUser(request.get("password"), request.get("token"));
        }

    }
}