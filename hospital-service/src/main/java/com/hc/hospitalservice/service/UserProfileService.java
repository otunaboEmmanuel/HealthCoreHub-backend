package com.hc.hospitalservice.service;

import com.hc.hospitalservice.dto.UserProfileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {
    @Value("${tenant.datasource.host}")
    private String tenantDbHost;

    @Value("${tenant.datasource.port}")
    private String tenantDbPort;

    @Value("${tenant.datasource.username}")
    private String tenantDbUsername;

    @Value("${tenant.datasource.password}")
    private String tenantDbPassword;
    public UserProfileDTO getUserProfile(String email, String tenantDb) {

        log.info("Fetching profile for: {} from {}", email, tenantDb);

        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);

        String userSql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(userSql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                throw new IllegalArgumentException("User not found: " + email);
            }
            // Build base profile
            UserProfileDTO profile = UserProfileDTO.builder()
                    .id(rs.getInt("id"))
                    .firstName(rs.getString("first_name"))
                    .middleName(rs.getString("middle_name"))
                    .lastName(rs.getString("last_name"))
                    .email(rs.getString("email"))
                    .phoneNumber(rs.getString("phone_number"))
                    .role(rs.getString("role"))
                    .profilePicture(rs.getString("profile_picture"))
                    .status(rs.getString("status"))
                    .build();
            // Get role-specific details
            Integer userId = profile.getId();
            String role = profile.getRole();

            Object roleDetails = getRoleSpecificDetails(userId, role, conn);
            profile.setRoleDetails(roleDetails);

            return profile;

        } catch (SQLException e) {
            log.error("❌ Error fetching user profile", e);
            throw new RuntimeException("Failed to fetch profile: " + e.getMessage());
        }
    }
    /**
     * Get role-specific details based on user's role
     */
    private Object getRoleSpecificDetails(Integer userId, String role, Connection conn) throws SQLException {

        String query = switch (role) {
            case "DOCTOR" -> """
                SELECT d.*, u.first_name, u.last_name
                FROM doctors d
                JOIN users u ON d.user_id = u.id
                WHERE d.user_id = ?
                """;
            case "NURSE" -> """
                SELECT n.*, u.first_name, u.last_name
                FROM nurses n
                JOIN users u ON n.user_id = u.id
                WHERE n.user_id = ?
                """;
            case "PATIENT" -> """
                SELECT p.*, u.first_name, u.last_name
                FROM patients p
                JOIN users u ON p.user_id = u.id
                WHERE p.user_id = ?
                """;
            case "PHARMACIST" -> """
                SELECT ph.*, u.first_name, u.last_name
                FROM pharmacists ph
                JOIN users u ON ph.user_id = u.id
                WHERE ph.user_id = ?
                """;
            case "LAB_SCIENTIST" -> """
                SELECT ls.*, u.first_name, u.last_name
                FROM laboratory_scientists ls
                JOIN users u ON ls.user_id = u.id
                WHERE ls.user_id = ?
                """;
            default -> null;
        };
        if (query == null) {
            return null;  // No role-specific table
        }

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToRoleObject(rs, role);
            }
        }

        return null;
    }
    /**
     * Map ResultSet to role-specific object
     */
    private Object mapResultSetToRoleObject(ResultSet rs, String role) throws SQLException {

        return switch (role) {
            case "DOCTOR" -> java.util.Map.of(
                    "id", rs.getInt("id"),
                    "specialization", rs.getString("specialization"),
                    "department", rs.getString("department"),
                    "licenseNumber", rs.getString("license_number"),
                    "licenseIssueDate", rs.getDate("license_issue_date"),
                    "licenseExpiryDate", rs.getDate("license_expiry_date"),
                    "licenseAuthority", rs.getString("license_authority")
            );
            case "NURSE" -> java.util.Map.of(
                    "id", rs.getInt("id"),
                    "specialization", rs.getString("specialization"),
                    "department", rs.getString("department"),
                    "licenseNumber", rs.getString("license_number"),
                    "shiftHours", rs.getString("shift_hours"),
                    "yearsOfExperience", rs.getInt("years_of_experience")
            );

            case "PATIENT" -> java.util.Map.of(
                    "id", rs.getInt("id"),
                    "patientId", rs.getString("patient_id"),
                    "hospitalNumber", rs.getString("hospital_number"),
                    "dateOfBirth", rs.getDate("date_of_birth"),
                    "bloodGroup", rs.getString("blood_group"),
                    "genotype", rs.getString("genotype"),
                    "gender", rs.getString("gender")
            );
            default -> null;
        };
}
}