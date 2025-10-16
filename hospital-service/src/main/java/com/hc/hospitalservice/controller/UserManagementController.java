package com.hc.hospitalservice.controller;

import com.hc.hospitalservice.dto.CreateUserRequest;
import com.hc.hospitalservice.dto.UserResponse;
import com.hc.hospitalservice.service.JwtService;
import com.hc.hospitalservice.service.UserManagementService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class UserManagementController {
    private final UserManagementService userManagementService;
    private final JwtService jwtService;

    /**
     * Create a new user (only admins can do this)
     */
    @PostMapping
    public ResponseEntity<?> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Extract JWT
            String token = authHeader.replace("Bearer ", "");
            Claims claims = jwtService.extractClaims(token);

            String tenantDb = claims.get("tenant_db", String.class);
            Integer hospitalId = claims.get("hospital_id", Integer.class);
            String tenantRole = claims.get("tenant_role", String.class);
            if (!"ADMIN".equals(tenantRole))
            {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Only admins can create users");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            UserResponse response = userManagementService.createUser(request, tenantDb, hospitalId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Validation error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("❌ Error creating user", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    @GetMapping
    public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Claims claims = jwtService.extractClaims(token);
            String tenantDb = claims.get("tenant_db", String.class);

            // TODO: Implement get all users from tenant DB

            Map<String, String> response = new HashMap<>();
            response.put("message", "Get all users - to be implemented");
            response.put("tenantDb", tenantDb);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch users"));
        }
    }
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "auth-service");
        return ResponseEntity.ok(response);
    }

}
