package com.hc.appointmentservice.controller;

import com.hc.appointmentservice.dto.DoctorDTO;
import com.hc.appointmentservice.dto.DoctorResponse;
import com.hc.appointmentservice.dto.UpdateDoctorRequest;
import com.hc.appointmentservice.entity.Appointment;
import com.hc.appointmentservice.service.DoctorService;
import com.hc.appointmentservice.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/doctor")
@RequiredArgsConstructor
@Slf4j
public class DoctorController {
    private final JwtService jwtService;
    private final DoctorService doctorService;
    @PostMapping("appointment/{id}")
    public ResponseEntity<?> setAvailability(@PathVariable Integer id,
                                             @RequestHeader("Authorization") String authHeader,
                                             @RequestBody UpdateDoctorRequest request) {
        try{
            //Extract token
            String token = authHeader.substring(7);
            Claims claims = jwtService.extractClaims(token);
            String tenantDb = claims.get("tenant_db", String.class);
            String tenant_role = claims.get("tenant_role", String.class);
            if(!("ADMIN".equals(tenant_role)) && !("DOCTOR".equals(tenant_role))){
                Map<String, String> error = new HashMap<>();
                error.put("error", "Only admins or doctors can create appointments");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            Map<String, String> response = doctorService.setDoctorAvailability(id, request, tenantDb);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }catch (Exception e) {
            log.error(" Error creating user", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    @GetMapping()
    public ResponseEntity<?> getAllDoctors(@RequestHeader("Authorization")String authHeader){
        try{
            String token = authHeader.substring(7);
            Claims claims = jwtService.extractClaims(token);
            String tenantDb = claims.get("tenant_db", String.class);
            String tenant_role = claims.get("tenant_role", String.class);
            if(!("ADMIN".equals(tenant_role))&& !("PATIENT".equals(tenant_role))){
                Map<String, String> error = new HashMap<>();
                error.put("error", "Only admins or users can see doctor lists");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            List<DoctorDTO> doctorDTO = doctorService.getAllDoctors(tenantDb);
            return ResponseEntity.ok(Map.of("doctors", doctorDTO,
                                        "size",doctorDTO.size()));
        }catch (Exception e)
        {
            log.error(" Error getting doctors", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get doctors: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    @GetMapping("{doctorId}")
    public ResponseEntity<?> getAppointmentById(@PathVariable Integer doctorId,
                                                @RequestHeader("Authorization")String authHeader) {
        try {
            String token = authHeader.substring(7);
            String tenantDb = jwtService.extractTenantDb(token);
            String tenant_role = jwtService.extractTenantRole(token);
            if (!("ADMIN".equals(tenant_role)) && !("DOCTOR".equals(tenant_role))) {
                log.error(" this role cant access endpoint {}", tenant_role);
                throw new RuntimeException(" this role cant access endpoint " + tenant_role);
            }
            List<DoctorResponse> doctorResponses = doctorService.getAppointments(doctorId, tenantDb);
            return ResponseEntity.ok(Map.of("doctors", doctorResponses));
        }catch (Exception e) {
            log.error(" Error getting doctors", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get doctors: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    //updateStatus
    @PutMapping("{}")
}
