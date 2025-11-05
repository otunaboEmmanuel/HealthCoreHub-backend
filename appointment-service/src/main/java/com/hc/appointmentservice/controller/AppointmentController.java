package com.hc.appointmentservice.controller;

import com.hc.appointmentservice.dto.AppointmentDTO;
import com.hc.appointmentservice.entity.Appointment;
import com.hc.appointmentservice.service.AppointmentService;
import com.hc.appointmentservice.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/appointment")
@Service
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final JwtService jwtService;;
    @PostMapping("{patientId}/{doctorId}")
    public ResponseEntity<?> addAppointment(@RequestBody AppointmentDTO appointment,
                                            @RequestHeader("Authorization")String authHeader,
                                            @PathVariable Integer patientId, @PathVariable Integer doctorId) {
        try {
            String token = authHeader.substring(7);
            String tenantDb = jwtService.extractTenantDb(token);
            String tenantRole = jwtService.extractTenantRole(token);
            if (!tenantRole.equalsIgnoreCase("admin") && !tenantRole.equalsIgnoreCase("patient")) {
                throw new RuntimeException("does not have access to this endpoint");
            }
            Appointment appointment1 = appointmentService.bookAppointment(appointment, tenantDb, patientId, doctorId);
            if (appointment1 == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("appointment not found");
            }
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "success",
                    "appointment", appointment1));
        }catch (IllegalArgumentException e) {
            log.warn("Invalid appointment request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Error occurred while adding appointment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to book appointment, token might be expired"));
        }
    }

    //update appointment
    @PutMapping("{patientId}")
    public ResponseEntity<?> updateAppointment(@RequestHeader("Authorization")String authHeader,
                                               @RequestBody Map<String, String> request,
                                               @PathVariable Integer patientId) {
        try {
            String token = authHeader.substring(7);
            String tenantRole = jwtService.extractTenantRole(token);
            if (!tenantRole.equalsIgnoreCase("admin") && !tenantRole.equalsIgnoreCase("patient")) {
                log.warn("Invalid appointment request: {}", token);
                throw new RuntimeException("does not have access to this endpoint");
            }
            Map<String, Object> response = appointmentService.updateAppointment(request, patientId);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }catch (IllegalArgumentException e) {
            log.warn("Invalid appointment request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }catch (Exception e) {
            log.error("Error occurred while updating appointment", e);
            throw  new RuntimeException("Error occurred while updating appointment");
        }
    }
    //get particular patient by email for appointment booking
    @GetMapping("{email}")
    public ResponseEntity<?> getAppointment(@PathVariable String email, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String tenantDb = jwtService.extractTenantDb(token);
            String tenantRole = jwtService.extractTenantRole(token);
            if (!tenantRole.equalsIgnoreCase("admin") && !tenantRole.equalsIgnoreCase("patient")) {
                log.warn("Invalid appointment request: {}", token);
                throw new RuntimeException("does not have access to this endpoint");
            }
            Map<String,Object> result = appointmentService.getPatientByEmail(email, tenantDb);
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }catch (IllegalArgumentException e) {
            log.warn("Invalid appointment request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }catch (Exception e) {
            log.error("Error occurred while getting email", e);
            throw  new RuntimeException("Error occurred while getting email");
        }
    }
    @GetMapping("{patientId}")
    public ResponseEntity<?> getAppointmentByPatientId(@PathVariable Integer patientId, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String tenantRole = jwtService.extractTenantRole(token);
            if (!tenantRole.equalsIgnoreCase("admin") && !tenantRole.equalsIgnoreCase("patient")) {
                log.warn("this token can't access this endpoint: {}", token);
                throw new RuntimeException("does not have access to this endpoint");
            }
            Map<String, Object> result = appointmentService.getAppointmentByPatient(patientId);
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }catch (IllegalArgumentException e) {
            log.warn("Invalid appointment request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }catch (Exception e) {
            log.error("Error occurred while getting appointment for patient", e);
            throw  new RuntimeException("Error occurred while getting appointment");
        }
    }
}
