package com.hc.hospitalservice.service;

import com.hc.hospitalservice.dto.CreateUserRequest;
import com.hc.hospitalservice.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.sql.*;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${tenant.datasource.host}")
    private String tenantDbHost;

    @Value("${tenant.datasource.port}")
    private String tenantDbPort;

    @Value("${tenant.datasource.username}")
    private String tenantDbUsername;

    @Value("${tenant.datasource.password}")
    private String tenantDbPassword;

    @Transactional(rollbackFor = Exception.class)
    public UserResponse createUser(CreateUserRequest request, String tenantDb, Integer hospitalId) {

        log.info("👤 Creating user: {} with role: {}", request.getEmail(), request.getRole());

        try {
            // Step 1: Register in Auth Service
            String authUserId = registerInAuthService(request, hospitalId, tenantDb);

            // Step 2: Create user in Tenant DB
            Integer tenantUserId = createUserInTenantDb(request, tenantDb, authUserId);
            if (shouldCreateRoleSpecificRecord(request.getRole())) {
                createRoleSpecificRecord(request, tenantUserId, tenantDb);
            }

            log.info("✅ User created successfully: {}", request.getEmail());

            return UserResponse.builder()
                    .success(true)
                    .message("User created successfully")
                    .userId(tenantUserId)
                    .authUserId(authUserId)
                    .email(request.getEmail())
                    .role(request.getRole())
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to create user: {}", request.getEmail(), e);
            throw new RuntimeException("User creation failed: " + e.getMessage(), e);
        }
    }

    private String registerInAuthService(CreateUserRequest request, Integer hospitalId, String tenantDb) {

        Map<String, Object> authRequest = Map.of(
                "email", request.getEmail(),
                "password", request.getPassword(),
                "hospitalId", hospitalId,
                "tenantDb", tenantDb,
                "globalRole", "HOSPITAL_USER"
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    authServiceUrl + "/auth/register",
                    authRequest,
                    Map.class
            );
            String authUserId = (String) response.getBody().get("userId");
            log.info("✅ User registered in auth service: {}", authUserId);
            return authUserId;

        } catch (Exception e) {
            log.error("❌ Auth service registration failed", e);
            throw new RuntimeException("Auth registration failed: " + e.getMessage());
        }
    }
    /**
     * Create user in tenant database
     */
    private Integer createUserInTenantDb(CreateUserRequest request, String tenantDb, String authUserId) throws SQLException {

        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        String sql = """
            INSERT INTO users (
                first_name, middle_name, last_name, email, phone_number,
                password, role, profile_picture, status, auth_user_id,
                created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """;

        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, request.getFirstName());
            stmt.setString(2, request.getMiddleName());
            stmt.setString(3, request.getLastName());
            stmt.setString(4, request.getEmail());
            stmt.setString(5, request.getPhoneNumber());
            stmt.setString(6, hashedPassword);
            stmt.setString(7, request.getRole());
            stmt.setString(8, request.getProfilePicture());
            stmt.setString(9, "ACTIVE");
            stmt.setObject(10, UUID.fromString(authUserId));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Integer userId = rs.getInt("id");
                log.info("✅ User created in tenant DB with ID: {}", userId);
                return userId;
            }

            throw new SQLException("Failed to create user in tenant DB");

        } catch (SQLException e) {
            log.error("❌ Tenant DB user creation failed", e);
            throw new RuntimeException("Tenant DB error: " + e.getMessage(), e);
        }
    }
    private boolean shouldCreateRoleSpecificRecord(String role) {
        return !role.equals("ADMIN") && !role.equals("STAFF");
    }
    private void createRoleSpecificRecord(CreateUserRequest request, Integer userId, String tenantDb) {

        switch (request.getRole()) {
            case "DOCTOR" -> createDoctorRecord(request.getDoctorDetails(), userId, tenantDb);
            case "NURSE" -> createNurseRecord(request.getNurseDetails(), userId, tenantDb);
            case "PATIENT" -> createPatientRecord(request.getPatientDetails(), userId, tenantDb);
            case "PHARMACIST" -> createPharmacistRecord(request.getPharmacistDetails(), userId, tenantDb);
            case "LAB_SCIENTIST" -> createLabScientistRecord(request.getLabScientistDetails(), userId, tenantDb);
            default -> log.warn("No role-specific table for role: {}", request.getRole());
        }
    }
    /**
     * Create doctor record
     */
    private void createDoctorRecord(CreateUserRequest.DoctorDetailsRequest details, Integer userId, String tenantDb) {

        if (details == null) {
            log.warn("Doctor details not provided for user: {}", userId);
            return;
        }

        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);

        String sql = """
            INSERT INTO doctors (
                user_id, specialization, department, license_number,
                license_issue_date, license_expiry_date, license_authority,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;
        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, details.getSpecialization());
            stmt.setString(3, details.getDepartment());
            stmt.setString(4, details.getLicenseNumber());
            stmt.setDate(5, Date.valueOf(details.getLicenseIssueDate()));
            stmt.setDate(6, Date.valueOf(details.getLicenseExpiryDate()));
            stmt.setString(7, details.getLicenseAuthority());

            stmt.executeUpdate();
            log.info("✅ Doctor record created for user: {}", userId);
        } catch (SQLException e) {
            log.error("❌ Failed to create doctor record", e);
            throw new RuntimeException("Doctor record creation failed: " + e.getMessage());
        }
    }
    /**
     * Create nurse record
     */
    private void createNurseRecord(CreateUserRequest.NurseDetailsRequest details, Integer userId, String tenantDb) {

        if (details == null) {
            log.warn("Nurse details not provided for user: {}", userId);
            return;
        }

        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);

        String sql = """
            INSERT INTO nurses (
                user_id, specialization, department, license_number,
                license_issue_date, license_expiry_date, shift_hours,
                years_of_experience, created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;
        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, details.getSpecialization());
            stmt.setString(3, details.getDepartment());
            stmt.setString(4, details.getLicenseNumber());
            stmt.setDate(5, Date.valueOf(details.getLicenseIssueDate()));
            stmt.setDate(6, Date.valueOf(details.getLicenseExpiryDate()));
            stmt.setString(7, details.getShiftHours());
            stmt.setInt(8, details.getYearsOfExperience());

            stmt.executeUpdate();
            log.info("✅ Nurse record created for user: {}", userId);

        } catch (SQLException e) {
            log.error("❌ Failed to create nurse record", e);
            throw new RuntimeException("Nurse record creation failed: " + e.getMessage());
        }
    }
    /**
     * Create patient record
     */
    private void createPatientRecord(CreateUserRequest.PatientDetailsRequest details, Integer userId, String tenantDb) {

        if (details == null) {
            log.warn("Patient details not provided for user: {}", userId);
            return;
        }

        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);

        String sql = """
            INSERT INTO patients (
                user_id, patient_id, hospital_number, date_of_birth,
                gender, blood_group, genotype, marital_status, occupation,
                country, state, city, address_line,
                next_of_kin_name, next_of_kin_relationship, next_of_kin_phone,
                emergency_contact_name, emergency_contact_phone,
                allergies, chronic_conditions,
                registration_date, created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;
        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
        PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, details.getPatientId());
            stmt.setString(3, details.getHospitalNumber());
            stmt.setDate(4, Date.valueOf(details.getDateOfBirth()));
            stmt.setString(5, details.getGender());
            stmt.setString(6, details.getBloodGroup());
            stmt.setString(7, details.getGenotype());
            stmt.setString(8, details.getMaritalStatus());
            stmt.setString(9, details.getOccupation());
            stmt.setString(10, details.getCountry());
            stmt.setString(11, details.getState());
            stmt.setString(12, details.getCity());
            stmt.setString(13, details.getAddress());
            stmt.setString(14, details.getNextOfKinName());
            stmt.setString(15, details.getNextOfKinRelationship());
            stmt.setString(16, details.getNextOfKinPhone());
            stmt.setString(17, details.getEmergencyContactName());
            stmt.setString(18, details.getEmergencyContactPhone());
            stmt.setString(19, details.getAllergies());
            stmt.setString(20, details.getChronicConditions());
            stmt.executeUpdate();
            log.info("✅ Patient record created for user: {}", userId);

        } catch (SQLException e) {
            log.error("❌ Failed to create patient record", e);
            throw new RuntimeException("Patient record creation failed: " + e.getMessage());
        }
    }
    private void createPharmacistRecord(CreateUserRequest.PharmacistDetailsRequest details,
                                        Integer userId,
                                        String tenantDb) {
        if (details == null) {
            log.warn("Pharmacist details not provided for user: {}", userId);
            return;
        }
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);

        String sql = """
        
                INSERT INTO pharmacists (
            user_id, license_number, license_issue_date, license_expiry_date,
            specialization, department, years_of_experience, license_authority, created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;
    try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
    PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setInt(1, userId);
        stmt.setString(2, details.getLicenseNumber());
        stmt.setDate(3, details.getLicenseIssueDate() != null ? Date.valueOf(details.getLicenseIssueDate()) : null);
        stmt.setDate(4, details.getLicenseExpiryDate() != null ? Date.valueOf(details.getLicenseExpiryDate()) : null);
        stmt.setString(5, details.getSpecialization());
        stmt.setString(6, details.getDepartment());
        stmt.setObject(7, details.getYearsOfExperience());
        stmt.setString(8, details.getLicenseAuthority());

        stmt.executeUpdate();
        log.info("✅ Pharmacist record created for user: {}",

        userId);

    } catch (SQLException e) {
        log.error("❌ Failed to create pharmacist record"
            , e);
        throw new RuntimeException("Pharmacist record creation failed: "
        + e.getMessage());
    }
    }
    private void createLabScientistRecord(CreateUserRequest.LabScientistDetailsRequest details,
                                          Integer userId,
                                          String tenantDb) {
        if (details == null) {
            log.warn("LabScientist details not provided for user: {}", userId);
            return;
        }
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);
        String sql = """
        INSERT INTO lab_scientists (
            user_id, license_number, license_issue_date, license_expiry_date,
            specialization, department, license_authority, years_of_experience, created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;
        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, details.getLicenseNumber());
            stmt.setDate(3, details.getLicenseIssueDate() != null ? Date.valueOf(details.getLicenseIssueDate()) : null);
            stmt.setDate(4, details.getLicenseExpiryDate() != null ? Date.valueOf(details.getLicenseExpiryDate()) : null);
            stmt.setString(5, details.getSpecialization());
            stmt.setString(6, details.getDepartment());
            stmt.setString(7, details.getLicenseAuthority());
            stmt.setObject(8, details.getYearsOfExperience());

            stmt.executeUpdate();
            log.info("✅ Lab scientist record created for user: {}", userId);

        } catch (SQLException e) {
            log.error("❌ Failed to create lab scientist record", e);
            throw new RuntimeException("Lab scientist record creation failed: " + e.getMessage());
        }
    }

}




