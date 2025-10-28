package com.hc.appointmentservice.service;

import com.hc.appointmentservice.dto.AppointmentDTO;
import com.hc.appointmentservice.entity.Appointment;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

@Service
public class AppointmentService {
    @Value("${tenant.datasource.host}")
    private String tenantDbHost;

    @Value("${tenant.datasource.port}")
    private String tenantDbPort;

    @Value("${tenant.datasource.username}")
    private String tenantDbUsername;

    @Value("${tenant.datasource.password}")
    private String tenantDbPassword;
    @Transactional(rollbackOn =  Exception.class)
    public Appointment bookAppointment(AppointmentDTO appointment, String tenantDb) {
        if (!userscriptsInTenantDb(tenantDb, appointment)){

        }

    }

    private boolean userscriptsInTenantDb(String tenantDb, AppointmentDTO appointment) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
        String sql = "SELECT 1 from users WHERE id = ?";
        try(Connection conn = DriverManager.getConnection(tenantUrl,tenantDbUsername, tenantDbPassword);
            PreparedStatement statement= conn.prepareStatement(sql))
    }
}
