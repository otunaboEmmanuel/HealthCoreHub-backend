package com.hc.onboardingservice.controller;

import com.hc.onboardingservice.service.TenantDatabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dev")  // Clear it's for development
@RequiredArgsConstructor
@CrossOrigin
public class DevController {

    private final TenantDatabaseService tenantDatabaseService;

    @DeleteMapping("/cleanup-tenant")
    public ResponseEntity<?> cleanupTenant(
            @RequestParam String dbName,
            @RequestParam String dbUser) {
        try {
            tenantDatabaseService.dropTenantDatabase(dbName, dbUser);
            return ResponseEntity.ok(Map.of(
                    "code", "00",
                    "message", "Tenant database dropped successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "99",
                    "message", e.getMessage()
            ));
        }
    }
}