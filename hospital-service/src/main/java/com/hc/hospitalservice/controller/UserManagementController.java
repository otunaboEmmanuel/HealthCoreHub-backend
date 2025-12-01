package com.hc.hospitalservice.controller;

import com.hc.hospitalservice.dto.*;
import com.hc.hospitalservice.service.UserManagementService;
import com.hc.hospitalservice.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final UserProfileService userProfileService;
    /**
     * Create a new user (only admins can do this)
     */
    @PostMapping()
    public ResponseEntity<?> createUser(
             @Valid @RequestBody CreateUserRequest request,
             @RequestHeader(value = "X-Hospital-Id", required = false) String hospitalId,
             @RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb,
             @RequestHeader("X-Tenant-Role") String tenantRole) {
        try {
            if (!"ADMIN".equals(tenantRole)&& !("DOCTOR".equals(tenantRole)) && !("NURSE".equals(tenantRole)))
            {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Only admins can create users");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            Map<String,Object> response = userManagementService.createUser(request, tenantDb, Integer.valueOf(hospitalId));

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
    @PutMapping("{userId}/photo")
    public ResponseEntity<?> updateProfilePhoto(@RequestParam(value = "profile_picture", required = false) MultipartFile file,
                                                @PathVariable Integer userId, @RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb,
                                                @RequestHeader("X-Tenant-Role") String tenantRole) {
        try {
            if (!"DOCTOR".equals(tenantRole) && !"ADMIN".equals(tenantRole)){
                log.warn(" Unauthorized role : {}", tenantRole);
                throw new IllegalArgumentException("Only doctors can set profile picture");
            }
            Map<String, Object> result = userManagementService.uploadProfilePicture(userId, file, tenantDb);
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }catch (IllegalArgumentException e) {
            log.warn(" Validation error: {}", e.getMessage());
            throw e;
        }catch (Exception e) {
            log.error(" Error updating profile picture ", e);
            throw new IllegalArgumentException("Failed to update user profile picture: " + e.getMessage());
        }
    }
    @GetMapping("download-photo/{userId}")
    public ResponseEntity<?> downloadProfilePhoto(@PathVariable Integer userId,
                                                  @RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb,
                                                  @RequestHeader("X-Tenant-Role") String tenantRole) {
        try {
            if (!"DOCTOR".equals(tenantRole) && !"ADMIN".equals(tenantRole)){
                log.warn(" Unauthorized role : {}", tenantRole);
                throw new IllegalArgumentException("Only doctors and admin can set profile picture");
            }
            String profile_picture = userManagementService.getProfilePicture(tenantDb, userId);
            return userManagementService.getProfilePictureResponse(profile_picture);
        }catch (IllegalArgumentException e) {
            log.warn(" Validation error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Only admins can download profile picture");
            return ResponseEntity.badRequest().body(error);
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
                                          @RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb,
                                          @RequestHeader("X-Tenant-Role") String tenantRole) {
        try {
            if (!"ADMIN".equals(tenantRole))
            {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Only admins can update status");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            Map<String, String> response = userProfileService.updateUser(id, tenantDb, request);
            return ResponseEntity.ok(response);
        }catch (IllegalArgumentException e) {
            log.warn(" Validation error: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error(" Error update  user", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

    }
    //get hospital number from patientId
    @GetMapping("{patientId}")
    public ResponseEntity<?> getHospitalNumber(@PathVariable Integer patientId,  @RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb,
                                               @RequestHeader("X-Tenant-Role") String tenantRole) {
        try{
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
    @GetMapping("active-patients")
    public ResponseEntity<?> getPatients( @RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb,
                                          @RequestHeader("X-Tenant-Role") String tenantRole) {
        try{
            if(!"ADMIN".equals(tenantRole))
            {
                log.warn("only admin can access this");
                throw new IllegalArgumentException("this user is not admin");
            }
            List<PatientDto> result = userManagementService.getPatients(tenantDb);
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }catch (IllegalArgumentException e) {
            log.warn("this user is not admin");
            Map<String, String> error = new HashMap<>();
            error.put("error", "Only admins can access this");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
        catch (Exception e) {
            log.error(" Error fetching patients", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch patients: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    @GetMapping()
    public ResponseEntity<?> getUsers(@RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb,
                                      @RequestHeader("X-Tenant-Role") String tenantRole){
        try{
            if(!"ADMIN".equals(tenantRole)){
                log.warn("only admin can access this");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only admins can access this"));
            }
            List<Map<String,Object>> result = userManagementService.getHospitalStaff(tenantDb);
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }catch (IllegalArgumentException e) {
            log.warn("this user is not admin");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @DeleteMapping("{userId}")
    public ResponseEntity<?> deleteUsers(@PathVariable Integer userId,@RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb,
                                         @RequestHeader("X-Tenant-Role") String tenantRole){
        log.info("deleting user with id {} from hospital {}", userId, tenantDb);
        try{
            if(!tenantRole.equals("ADMIN")){
                log.warn("only admin can access this specific endpoint");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only admins can access this endpoint"));
            }
            Map<String, Object> response = userManagementService.deleteUser(tenantDb,userId);
        }
    }
}
