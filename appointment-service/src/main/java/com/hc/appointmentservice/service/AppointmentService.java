package com.hc.appointmentservice.service;

import com.hc.appointmentservice.dto.AppointmentDTO;
import com.hc.appointmentservice.entity.Appointment;
import com.hc.appointmentservice.enums.Status;
import com.hc.appointmentservice.repository.AppointmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentService {
    @Value("${tenant.datasource.host}")
    private String tenantDbHost;

    @Value("${tenant.datasource.port}")
    private String tenantDbPort;

    @Value("${tenant.datasource.username}")
    private String tenantDbUsername;

    @Value("${tenant.datasource.password}")
    private String tenantDbPassword;
    private final AppointmentRepository appointmentRepository;

    @Transactional(rollbackFor = Exception.class)
    public Appointment bookAppointment(AppointmentDTO appointment, String tenantDb) {
        if(!userExistsInTenant(tenantDb,appointment.getPatientId())){
            log.error("could not find not find id {} in users table",appointment.getPatientId());
            throw new RuntimeException("user with id " + appointment.getPatientId() + " not exists");
        }
        if(!doctorExists(tenantDb,appointment.getDoctorId())){
            log.error("doctor with id {} not exists",appointment.getDoctorId());
            throw new RuntimeException("doctor with id " + appointment.getDoctorId() + " not exists");
        }
        Appointment appointment1 = Appointment.builder()
                .appointmentTime(appointment.getAppointmentTime())
                .date(appointment.getDate())
                .doctorId(appointment.getDoctorId())
                .patientId(appointment.getPatientId())
                .reason(appointment.getReason())
                .status(Status.PENDING)
                .build();
         return appointmentRepository.save(appointment1);
    }
    private boolean userExistsInTenant(String tenantDb, Integer userId) {
        String tenantUrl=String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
        String sql = "SELECT COUNT (*) FROM patients WHERE id = ?";
        try(Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername,tenantDbPassword);
                                PreparedStatement statement = conn.prepareStatement(sql) ){
            statement.setInt(1,userId);
            ResultSet rs = statement.executeQuery();
            return rs.next() && rs.getInt(1) >0;
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
    private boolean doctorExists(String tenantDb, Integer doctorId) {
        String tenantUrl=String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
        String sql = "SELECT COUNT(*) FROM doctors WHERE id = ?";
        try(Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername,tenantDbPassword);
            PreparedStatement statement = conn.prepareStatement(sql) ){
            statement.setInt(1,doctorId);
            ResultSet rs = statement.executeQuery();
            return rs.next() && rs.getInt(1) >0;
        } catch (SQLException e) {
            log.error(" Error checking user existence in tenant {} ", tenantDb);
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> updateAppointment(Map<String, String> request, Integer patientId) {
        Map<String, Object> response = new HashMap<>();
        if(!appointmentRepository.existsByPatientId(patientId)){
            log.error("could not find id {} in appointment table",patientId);
            throw new RuntimeException("appointment with id " + patientId + " not exists");
        }
        Appointment appointment = appointmentRepository.findByPatientId(patientId).orElse(null);
        if(appointment != null) {
            appointment.setAppointmentTime(request.get("appointmentTime"));
            appointment.setDate(LocalDate.parse(request.get("date")));
            appointmentRepository.save(appointment);
            response.put("status", "00");
            response.put("message", "appointment updated successfully");
            return response;
        }
        response.put("status", "error");
        response.put("message", "appointment not found");
        return response;
    }


}
