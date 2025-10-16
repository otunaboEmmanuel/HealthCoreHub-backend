package com.hc.hospitalservice.controller;

import com.hc.hospitalservice.dto.UserProfileDTO;
import com.hc.hospitalservice.service.JwtService;
import com.hc.hospitalservice.service.UserProfileService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {
    private final JwtService jwtService;
    private final UserProfileService userProfileService;

    @GetMapping()
    public ResponseEntity<?> getMyProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Claims claims = jwtService.extractClaims(token);

            String email = claims.getSubject();
            String tenantDb = claims.get("tenant_db", String.class);

            UserProfileDTO profile = userProfileService.getUserProfile(email, tenantDb);

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            log.error("❌ Error fetching profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch profile"));
        }
    }
    @GetMapping("/{email}")
    public ResponseEntity<?> getUserProfile(
            @PathVariable String email,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.replace("Bearer ", "");
            Claims claims = jwtService.extractClaims(token);
            String tenantDb = claims.get("tenant_db", String.class);

            UserProfileDTO profile = userProfileService.getUserProfile(email, tenantDb);

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            log.error("❌ Error fetching profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch profile"));
        }
    }
}
