package com.hc.appointmentservice.service;

import com.hc.appointmentservice.dto.AppointmentDTO;
import com.hc.appointmentservice.entity.Appointment;
import com.hc.appointmentservice.enums.Status;
import com.hc.appointmentservice.repository.AppointmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;

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
        if(!userExistsInTenant(tenantDb,appointment.getUserId())){
            log.error("could not find not find id {} in users table",appointment.getUserId());
            throw new RuntimeException("user with id " + appointment.getUserId() + " not exists");
        }
        if(!doctorExists(tenantDb,appointment.getDoctorId())){
            log.error("doctor with id {} not exists",appointment.getDoctorId());
            throw new RuntimeException("doctor with id " + appointment.getDoctorId() + " not exists");
        }
        Appointment appointment1 = Appointment.builder()
                .appointmentTime(appointment.getAppointmentTime())
                .date(appointment.getDate())
                .doctorId(appointment.getDoctorId())
                .userId(appointment.getUserId())
                .reason(appointment.getReason())
                .status(Status.PENDING)
                .build();
         return appointmentRepository.save(appointment1);
    }
    private boolean userExistsInTenant(String tenantDb, Integer userId) {
        String tenantUrl=String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
        String sql = "SELECT COUNT (*) FROM users WHERE id = ?";
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
}
