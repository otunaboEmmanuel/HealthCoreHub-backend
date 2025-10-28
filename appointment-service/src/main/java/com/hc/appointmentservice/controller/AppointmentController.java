package com.hc.appointmentservice.controller;

import com.hc.appointmentservice.dto.AppointmentDTO;
import com.hc.appointmentservice.entity.Appointment;
import com.hc.appointmentservice.service.AppointmentService;
import com.hc.appointmentservice.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/appointment")
@Service
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final JwtService jwtService;;
    @PostMapping()
    public ResponseEntity<?> addAppointment(@RequestBody AppointmentDTO appointment,
                                            @RequestHeader("Authorization")String authHeader) {
        try{
            String token = authHeader.substring(7);
            String tenantDb = jwtService.extractTenantDb(token);
            String tenantRole = jwtService.extractTenantRole(token);
            if(!tenantRole.equalsIgnoreCase("admin")&&!tenantRole.equalsIgnoreCase("patient")){
                log.info("this role is not allowed to reach this endpoint {}", tenantRole);
                throw new RuntimeException("does not have access to this endpoint");
            }
            Appointment appointment1= appointmentService.bookAppointment(appointment, tenantDb);

        }

    }
}
