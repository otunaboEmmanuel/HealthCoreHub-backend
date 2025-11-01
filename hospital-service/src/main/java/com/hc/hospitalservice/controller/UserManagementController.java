package com.hc.hospitalservice.controller;

import com.hc.hospitalservice.dto.*;
import com.hc.hospitalservice.service.JwtService;
import com.hc.hospitalservice.service.UserManagementService;
import com.hc.hospitalservice.service.UserProfileService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserManagementController {
    private final UserManagementService userManagementService;
    private final JwtService jwtService;
    private final UserProfileService userProfileService;

    /**
     * Create a new user (only admins can do this)
     */
    @PostMapping()
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
            log.warn(" Validation error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error(" Error creating user", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    @PostMapping("/register")
    public ResponseEntity<?> registerPatient(@Valid @RequestBody PatientRequest request) {

        log.info(" Patient registration request: {}", request.getEmail());

        try {
            UserResponse response = userManagementService.registerPatient(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn(" Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error(" Registration error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Registration failed. Please try again later."
            ));
        }
    }
    @GetMapping("/hospitals")
    public ResponseEntity<?> getAvailableHospitals() {
        try {
            List<HospitalListDto> hospitals = userManagementService.getHospitalList();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", hospitals.size(),
                    "hospitals", hospitals
            ));

        } catch (Exception e) {
            log.error(" Error fetching hospitals", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to fetch hospitals"
            ));
        }
    }
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "auth-service");
        return ResponseEntity.ok(response);
    }
    @PutMapping("{id}")
    public ResponseEntity<?> updateStatus(@PathVariable Integer id,
                                           @RequestBody UpdateRequest request,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Claims claims = jwtService.extractClaims(token);
            String tenantDb = claims.get("tenant_db", String.class);
            String tenantRole = claims.get("tenant_role", String.class);
            if (!"ADMIN".equals(tenantRole))
            {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Only admins can create users");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            Map<String, String> response = userProfileService.updateUser(id, tenantDb, request);
            return ResponseEntity.ok(response);
        }catch (IllegalArgumentException e) {
            log.warn("⚠️ Validation error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error(" Error creating user", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

    }
    //get hospital number from patientId
    @GetMapping("{patientId}")
    public ResponseEntity<?> getHospitalNumber(@PathVariable Integer patientId, @RequestHeader("Authorization")String authHeader) {
        try{
            String token = authHeader.substring(7);
            String tenantDb = jwtService.extractTenantDb(token);
            String tenantRole = jwtService.extractTenantRole(token);
            if(!"ADMIN".equals(tenantRole))
            {
                log.warn("this user is not admin");
                throw new IllegalArgumentException("this user is not admin");
            }
            Map<String, Object> result = userManagementService.getHospitalNumber(tenantDb, patientId);
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }catch (Exception e) {
            log.error(" Error fetching hospital number", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch hospital number: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }



}
