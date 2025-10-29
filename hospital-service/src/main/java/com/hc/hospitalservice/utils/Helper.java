package com.hc.hospitalservice.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class Helper {
    @Value("${tenant.datasource.username}")
    private String tenantDbUsername;

    @Value("${tenant.datasource.password}")
    private String tenantDbPassword;
    @Value("${tenant.datasource.host}")
    private String tenantDbHost;

    @Value("${tenant.datasource.port}")
    private String tenantDbPort;
    public static Long random() {
        return ThreadLocalRandom.current()
                .nextLong(1_000_000_000L, 10_000_000_000L); // 10 billion is exclusive
    }
    public static String generateRandomString() {
        return String.valueOf(
                ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L)
        );
    }
    public Connection getTenantConnection(String tenantDb,String sql) throws SQLException {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);
        Connection con = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
        PreparedStatement statement = con.prepareStatement(sql);
    }

}
