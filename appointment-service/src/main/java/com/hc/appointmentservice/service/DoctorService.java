package com.hc.appointmentservice.service;

import com.hc.appointmentservice.dto.UpdateDoctorRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DoctorService {

    @Value("${tenant.datasource.host}")
    private String tenantDbHost;

    @Value("${tenant.datasource.port}")
    private String tenantDbPort;

    @Value("${tenant.datasource.username}")
    private String tenantDbUsername;

    @Value("${tenant.datasource.password}")
    private String tenantDbPassword;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> setAppointment(Integer id, UpdateDoctorRequest request, String tenantDb) {
        if (!checkIdExist(id, tenantDb)) {
            throw new IllegalArgumentException("Doctor with id " + id + " does not exist");
        }

        setDoctorAppointment(id, request, tenantDb);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Doctor has been updated successfully");
        return response;
    }

    private void setDoctorAppointment(Integer id, UpdateDoctorRequest request, String tenantDb) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);

        String sql = """
                UPDATE doctors
                SET availability = ?::jsonb
                WHERE id = ?
                """;

        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, String.valueOf(request.getAvailability()));
            stmt.setInt(2, id);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new IllegalStateException("No doctor found with id " + id);
            }

            log.info("Updated doctor {} availability. Rows affected: {}", id, rowsAffected);

        } catch (SQLException exception) {
            log.error("Error updating doctor appointment for id: {}", id, exception);
            throw new RuntimeException("Failed to update doctor availability: " + exception.getMessage(), exception);
        }
    }

    private boolean checkIdExist(Integer id, String tenantDb) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);

        String sql = "SELECT 1 FROM doctors WHERE id = ?";

        try (Connection connection = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            log.error("Error checking if doctor exists with id: {}", id, e);
            throw new RuntimeException("Database error while checking doctor existence", e);
        }
    }
}