package com.hc.authservice.controller;

import com.hc.authservice.dto.LoginRequest;
import com.hc.authservice.dto.LoginResponse;
import com.hc.authservice.dto.RegisterRequest;
import com.hc.authservice.service.AuthService;
import com.hc.authservice.service.CookieService;
import com.hc.authservice.service.JwtService;
import com.hc.authservice.service.UserActivationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class AuthController {

    private final AuthService authService;
    private final UserActivationService userActivationService;
    private final CookieService cookieService;
    private final JwtService jwtService;

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
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        log.info(" Login request for: {}", request.getEmail());

        try {
            Map<String,Object> result = authService.login(request,response);
            return ResponseEntity.ok(result);

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
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "auth-service");
        return ResponseEntity.ok(response);
    }
    //validate token from email request---> tested ✅
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
    //tested ✅
    @PostMapping("activate")
    public ResponseEntity<?> activateUser(@RequestBody Map<String, String> request) {
        try{
            Map<String, Object> result = userActivationService.activateUser(request.get("password"), request.get("token"));
            return ResponseEntity.ok(result);
        }catch (IllegalArgumentException e) {
            log.warn(" Activation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }catch (Exception e) {
            log.error(" Activation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }

    }
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = cookieService.getRefreshTokenFromCookie(request)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        Map<String,Object> loginResponse = authService.refreshToken(refreshToken, response);
        return ResponseEntity.ok(loginResponse);
    }
    /**
     * Logout - clears cookies
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Logged out successfully"
        ));
    }
    @GetMapping("me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        try {
            String accessToken = cookieService.getAccessTokenFromCookie(request)
                    .orElseThrow(() -> new IllegalArgumentException("Access token not found"));
            Map<String, Object> result = authService.Me(accessToken);
            return ResponseEntity.ok(result);
        }catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }


}