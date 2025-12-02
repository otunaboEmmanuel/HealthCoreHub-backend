package com.hc.hospitalservice.controller;

import com.hc.hospitalservice.dto.PatientDto;
import com.hc.hospitalservice.dto.UserProfileDTO;
import com.hc.hospitalservice.service.UserProfileService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {
    private final UserProfileService userProfileService;
    @GetMapping()
    public ResponseEntity<?> getMyProfile(@RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb,
                                          @RequestHeader("X-Email") String email) {
        try {
            UserProfileDTO profile = userProfileService.getUserProfile(email, tenantDb);

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            log.error(" Error fetching profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch profile"));
        }
    }

    @GetMapping("/{email}")
    public ResponseEntity<?> getUserProfile(
            @PathVariable String email,
            @RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb) {

        try {
            UserProfileDTO profile = userProfileService.getUserProfile(email, tenantDb);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error(" Error fetching profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch profile"));
        }
    }

    @GetMapping("pending")
    public ResponseEntity<?> getPendingProfile(@RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb,
                                               @RequestHeader("X-Tenant-Role") String tenantRole) {
        try {
            if(!"ADMIN".equals(tenantRole)&& !("DOCTOR".equals(tenantRole))&& !("NURSE".equals(tenantRole))){
                log.error(" Error fetching profile");
                Map<String, String> error = new HashMap<>();
                error.put("error", "Only admin, nurse, doctor can access endpoint");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            List<PatientDto> patients = userProfileService.getPendingPatients(tenantDb);
            return ResponseEntity.ok(patients);
        } catch (Exception e) {
            log.error(" Error fetching profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch patients"));
        }
    }
    @PutMapping("{patientId}")
    public ResponseEntity<?> patientUpdateRecord(@PathVariable Integer patientId, @RequestBody Map<String, Object> patientUpdateRequest,
                                                 @RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb,
                                                 @RequestHeader("X-Tenant-Role") String tenantRole) {
        try{
            log.info("patient update request: {}", patientUpdateRequest);
            if(!"ADMIN".equals(tenantRole)&& !("PATIENT".equals(tenantRole))){
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("only admin and patients can access endpoint");
            }
            Map<String, Object> response = userProfileService.updatePatientRecord(patientUpdateRequest,tenantDb, patientId);
            return ResponseEntity.ok(response);
        }catch (Exception e){
            log.error(" Error fetching profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @PutMapping("update/{patientId}")
    public ResponseEntity<?> patientUpdateRecordAsStaff(@PathVariable Integer patientId, @RequestBody Map<String, Object> patientUpdateRequest,
                                                 @RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb,
                                                 @RequestHeader("X-Tenant-Role") String tenantRole) {
        try{
            log.info("patient update request: {}", patientUpdateRequest);
            if(!"ADMIN".equals(tenantRole)&& !("DOCTOR".equals(tenantRole))){
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("only admin and doctors can access endpoint");
            }
            Map<String, Object> response = userProfileService.updatePatientRecordAsStaff(patientUpdateRequest,tenantDb, patientId);
            return ResponseEntity.ok(response);
        }catch (Exception e){
            log.error(" Error fetching profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

}
