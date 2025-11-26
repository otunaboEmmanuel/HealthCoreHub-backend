package com.hc.appointmentservice.controller;

import com.hc.appointmentservice.dto.AppointmentDTO;
import com.hc.appointmentservice.dto.DoctorResponse;
import com.hc.appointmentservice.dto.PatientDto;
import com.hc.appointmentservice.entity.Appointment;
import com.hc.appointmentservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/appointment")
@Service
public class AppointmentController {
    private final AppointmentService appointmentService;
    @PostMapping("{patientId}/{doctorId}")
    public ResponseEntity<?> addAppointment(@RequestBody AppointmentDTO appointment,
                                            @PathVariable Integer patientId, @PathVariable Integer doctorId,
                                            @RequestHeader("X-Tenant-Role") String tenantRole,
                                            @RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb){
        try {
            if (!tenantRole.equalsIgnoreCase("admin") && !tenantRole.equalsIgnoreCase("patient")) {
                throw new RuntimeException("does not have access to this endpoint");
            }
            Appointment appointment1 = appointmentService.bookAppointment(appointment, tenantDb, patientId, doctorId);
            if (appointment1 == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("appointment not found");
            }
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "success",
                    "appointment", appointment1));
        }catch (Exception e){
            Map<String,Object> map = new HashMap<>();
            map.put("message", "this appointment time is not available");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
        }
    }

    //update appointment
    @PutMapping("{appointmentId}")
    public ResponseEntity<?> updateAppointment(@RequestBody Map<String, String> request,
                                               @PathVariable Integer appointmentId, @RequestHeader("X-Tenant-Role") String tenantRole) {
        try {
            if (!tenantRole.equalsIgnoreCase("admin") && !tenantRole.equalsIgnoreCase("patient")) {
                log.warn("Invalid appointment request for : {}", tenantRole);
                throw new RuntimeException("does not have access to this endpoint");
            }
            Map<String, Object> response = appointmentService.updateAppointment(request, appointmentId);
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
    public ResponseEntity<?> getAppointment(@PathVariable String email,
                                            @RequestHeader("X-Tenant-Role") String tenantRole,
                                            @RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb) {
        try {
            if (!tenantRole.equalsIgnoreCase("admin") && !tenantRole.equalsIgnoreCase("patient")) {
                log.warn("Invalid appointment request for: {}", tenantRole);
                throw new RuntimeException("does not have access to this endpoint");
            }
            PatientDto result = appointmentService.getPatientByEmail(email, tenantDb);
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }catch (IllegalArgumentException e) {
            log.warn("Invalid email for request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }catch (Exception e) {
            log.error("Error occurred while getting email", e);
            throw  new RuntimeException("Error occurred while getting email");
        }
    }
    @GetMapping("patient/{patientId}")
    public ResponseEntity<?> getAppointmentByPatientId(@PathVariable Integer patientId,
                                                       @RequestHeader("X-Tenant-Role") String tenantRole,
                                                       @RequestHeader(value = "X-Tenant-Db", required = false) String tenantDb) {
        try {
            if (!tenantRole.equalsIgnoreCase("admin") && !tenantRole.equalsIgnoreCase("patient")) {
                log.warn("this token can't access this endpoint: {}", tenantRole);
                throw new RuntimeException("does not have access to this endpoint");
            }
            List<DoctorResponse> result = appointmentService.getAppointmentByPatient(patientId, tenantDb);
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }catch (IllegalArgumentException e) {
            log.warn("Invalid patientId: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No appointments found for patient id " + patientId));
        }catch (Exception e) {
            log.error("Error occurred while getting appointment for patient", e);
            throw  new RuntimeException("Error occurred while getting appointment");
        }
    }
}
