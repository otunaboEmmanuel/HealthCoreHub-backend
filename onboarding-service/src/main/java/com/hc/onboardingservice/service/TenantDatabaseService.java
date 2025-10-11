package com.hc.onboardingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantDatabaseService {

        @Value("${spring.datasource.url}")
        private String masterUrl;

        @Value("${spring.datasource.username}")
        private String masterUser;

        @Value("${spring.datasource.password}")
        private String masterPassword;

    public void createTenantDatabase(String dbName, String dbUser, String dbPassword) {
        try (Connection connection = DriverManager.getConnection(masterUrl, masterUser, masterPassword);
             Statement stmt = connection.createStatement()) {

            // Check if database exists
            var rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'"
            );

            if (!rs.next()) {
                // Database doesn't exist, create it
                stmt.executeUpdate("CREATE DATABASE " + dbName);
                log.info("✅ Created database '{}'", dbName);
            } else {
                log.warn("⚠️ Database '{}' already exists, skipping creation", dbName);
            }

            // Check if user exists
            rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_user WHERE usename = '" + dbUser + "'"
            );

            if (!rs.next()) {
                // User doesn't exist, create it
                stmt.executeUpdate("CREATE USER " + dbUser + " WITH PASSWORD '" + dbPassword + "'");
                log.info("✅ Created user '{}'", dbUser);
            } else {
                log.warn("⚠️ User '{}' already exists, skipping creation", dbUser);
            }

            // Always grant privileges (idempotent operation)
            stmt.executeUpdate("GRANT ALL PRIVILEGES ON DATABASE " + dbName + " TO " + dbUser);
            log.info("✅ Granted database privileges to '{}'", dbUser);

        } catch (SQLException e) {
            log.error("❌ Error creating tenant database: {}", e.getMessage());
            throw new RuntimeException("Failed to create tenant database: " + e.getMessage());
        }

        // Grant schema privileges
        String tenantUrl = masterUrl.substring(0, masterUrl.lastIndexOf("/") + 1) + dbName;
        try (Connection tenantConn = DriverManager.getConnection(tenantUrl, masterUser, masterPassword);
             Statement stmt = tenantConn.createStatement()) {

            stmt.executeUpdate("GRANT ALL ON SCHEMA public TO " + dbUser);
            stmt.executeUpdate("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO " + dbUser);
            stmt.executeUpdate("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO " + dbUser);

            log.info("✅ Granted schema privileges to '{}'", dbUser);

        } catch (SQLException e) {
            log.error("❌ Error granting schema privileges: {}", e.getMessage());
            throw new RuntimeException("Failed to grant schema privileges: " + e.getMessage());
        }
    }

    public void initializeTenantSchema(String dbName, String dbUser, String dbPassword) {
        String tenantUrl = masterUrl.substring(0, masterUrl.lastIndexOf("/") + 1) + dbName;

        Flyway flyway = Flyway.configure()
                .dataSource(tenantUrl, dbUser, dbPassword)
                .locations("classpath:db/migration/tenants")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
        log.info("✅ Tenant schema initialized for database '{}'", dbName);
    }
    public void dropTenantDatabase(String dbName, String dbUser) {
        try (Connection connection = DriverManager.getConnection(masterUrl, masterUser, masterPassword);
             Statement stmt = connection.createStatement()) {

            // Terminate existing connections to the database
            String terminateQuery = "SELECT pg_terminate_backend(pid) FROM pg_stat_activity " +
                    "WHERE datname = '" + dbName + "' AND pid <> pg_backend_pid()";
            stmt.execute(terminateQuery);
            log.info("🔌 Terminated connections to database '{}'", dbName);

            // Drop database
            stmt.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
            log.info("✅ Dropped database '{}'", dbName);

            // Drop user
            stmt.executeUpdate("DROP USER IF EXISTS " + dbUser);
            log.info("✅ Dropped user '{}'", dbUser);

        } catch (SQLException e) {
            log.error("❌ Error dropping tenant database: {}", e.getMessage());
            throw new RuntimeException("Failed to drop tenant database: " + e.getMessage());
        }
    }
}
