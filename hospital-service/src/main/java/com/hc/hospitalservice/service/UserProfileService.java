package com.hc.hospitalservice.service;

import com.hc.hospitalservice.dto.PatientDto;
import com.hc.hospitalservice.dto.UpdateRequest;
import com.hc.hospitalservice.dto.UserProfileDTO;
import com.hc.hospitalservice.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {
    private final EmailService emailService;
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

    public List<PatientDto> getPendingPatients(String tenantDb) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);

        String sql = """
                SELECT
                    p.id,
                u.first_name,
                u.middle_name,
                u.last_name,
                u.email,
                u.phone_number,
                u.role,
                u.created_at,
                u.status,
                p.hospital_number
                FROM patients p
                INNER JOIN users u ON p.user_id = u.id
                WHERE u.status = ?
                """;
                List<PatientDto> patientDtoList=new ArrayList<>();
                try(Connection con = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
                                        PreparedStatement stmt = con.prepareStatement(sql)){
                    stmt.setString(1, "PENDING");
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()){
                        PatientDto patientDto=PatientDto.builder()
                                .id(rs.getInt("id"))
                                .firstName(rs.getString("first_name"))
                                .lastName(rs.getString("last_name"))
                                .middleName(rs.getString("middle_name"))
                                .email(rs.getString("email"))
                                .phoneNumber(rs.getString("phone_number"))
                                .role(rs.getString("role"))
                                .createdAt(rs.getObject("created_at", LocalDateTime.class))
                                .hospitalNumber(rs.getString("hospital_number"))
                                .status(rs.getString("status"))
                                .build();
                        patientDtoList.add(patientDto);
                    }
                    log.info("the patients with status pending are {}", patientDtoList);
                    return patientDtoList;
                } catch (SQLException e) {
                    log.error(" Failed to fetch patients", e);
                    throw new RuntimeException("Failed to fetch patients with status pending");
                }

            }

            @Transactional(rollbackFor = Exception.class)
            public Map<String, String> updateUser(Integer id, String tenantDb, UpdateRequest request) {
                try {
                    if (!existsIdInTenantDb(id, tenantDb)) {
                        throw new IllegalArgumentException("User with this ID does not exist in this hospital");
                    }
                    if(request.getStatus().equalsIgnoreCase("REJECTED")) {
                        deleteUserInTenantDb(id, tenantDb);
                    }
                    PatientDto updatedUser = updateUserInTenantDb(request, id, tenantDb);

                    String hospitalName = getHospitalNameFromTenantDb(id);
                    emailService.sendEmail(request.getEmail(),request.getFirstName(), hospitalName);

                    Map<String, String> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "User updated successfully");
                    response.put("updatedStatus", updatedUser.getStatus());
                    response.put("userEmail", updatedUser.getEmail());
                    return response;

                } catch (IllegalArgumentException e) {
                    log.warn(" Validation failed: {}", e.getMessage());
                    throw e;
                } catch (Exception e) {
                    log.error(" Failed to update user in tenant DB", e);
                    throw new RuntimeException("Database update failed: " + e.getMessage());
                }
            }

            private String getHospitalNameFromTenantDb(Integer id) {
                String tenantUrl = String.format("jdbc:postgresql://%s:%s/onboardingdb", tenantDbHost, tenantDbPort);
                String sql = "SELECT name FROM hospital  WHERE id = ? AND is_active = true";
                try(Connection conn = DriverManager.getConnection(tenantUrl,tenantDbUsername,tenantDbPassword);
                                            PreparedStatement statement = conn.prepareStatement(sql) ){
                    statement.setInt(1,id);
                    ResultSet rs = statement.executeQuery();
                    if(rs.next()){
                        return rs.getString("name");
                    }
                    return null;
                } catch (SQLException e) {
                    log.error(" Failed to fetch hospital name from DB", e);
                    throw new RuntimeException(e);
                }
            }

            private PatientDto updateUserInTenantDb(UpdateRequest request, Integer id, String tenantDb) {
                String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost,tenantDbPort,tenantDb);
                String sql = """
                UPDATE users
                        SET status = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        RETURNING id, first_name, middle_name, last_name, email, phone_number, role, status
                """;
        try(Connection con = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
            PreparedStatement stmt = con.prepareStatement(sql)){
            stmt.setString(1,request.getStatus());
            stmt.setInt(2, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()){
                return PatientDto.builder()
                        .id(rs.getInt("id"))
                        .firstName(rs.getString("first_name"))
                        .middleName(rs.getString("middle_name"))
                        .lastName(rs.getString("last_name"))
                        .email(rs.getString("email"))
                        .phoneNumber(rs.getString("phone_number"))
                        .role(rs.getString("role"))
                        .status(rs.getString("status"))
                        .build();
            } else {
                throw new SQLException("Failed to update user status");
            }

        } catch (SQLException e) {
            log.error(" Failed to fetch hospitals", e);
            throw new RuntimeException("Failed to fetch hospitals");
        }
    }

    private Boolean existsIdInTenantDb(Integer id, String tenantDb) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
        String sql = "SELECT 1 FROM users WHERE id = ?";
        try(Connection con = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
            PreparedStatement stmt = con.prepareStatement(sql)){
            stmt.setInt(1,id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }catch (SQLException e) {
            log.error("Error checking email", e);
            return false;
        }
    }
    private void deleteUserInTenantDb(Integer id, String tenantDb) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                log.warn("No user found with id={} in tenant {}", id, tenantDb);
                throw new IllegalArgumentException("User with this ID not found or already deleted");
            }

            log.info(" Deleted user with id={} from tenant {}", id, tenantDb);

        } catch (SQLException e) {
            log.error(" Failed to delete user in tenant DB", e);
            throw new RuntimeException("Error deleting user: " + e.getMessage());
        }
    }



}