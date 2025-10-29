package com.hc.hospitalservice.service;

import com.hc.hospitalservice.dto.*;
import com.hc.hospitalservice.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.sql.Date;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final EmailService emailService;
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
    @Value("${file.upload.directory:/app/uploads}")
    private String uploadDirectory;

    @Transactional(rollbackFor = Exception.class)
    public UserResponse createUser(CreateUserRequest request, String tenantDb, Integer hospitalId, MultipartFile file) {


        log.info("👤 Creating user: {} with role: {}", request.getEmail(), request.getRole());

        try {
            // Step 1: Register in Auth Service
            String authUserId = registerInAuthService(request, hospitalId, tenantDb);

            String profile_picture=saveFileToStorage(file);

            // Step 2: Create user in Tenant DB
            Integer tenantUserId = createUserInTenantDb(request, tenantDb, authUserId, profile_picture);
            Integer staffId = null;
            if (shouldCreateRoleSpecificRecord(request.getRole())) {
                staffId = createRoleSpecificRecord(request, tenantUserId, tenantDb);
            }

            log.info("✅ User created successfully: {}", request.getEmail());

            return UserResponse.builder()
                    .success(true)
                    .message("User created successfully")
                    .userId(tenantUserId)
                    .authUserId(authUserId)
                    .staffId(staffId)
                    .email(request.getEmail())
                    .role(request.getRole())
                    .build();

        } catch (Exception e) {
            log.error("Failed to create user: {}", request.getEmail(), e);
            throw new RuntimeException("User creation failed: " + e.getMessage(), e);
        }
    }

    public String saveFileToStorage(MultipartFile file){
        String extensionType=file.getContentType();//image/png

        String extension= "";
        if (!extensionType.isEmpty()) {
            String[] parts = extensionType.split("/");
            if (parts.length > 1) {
                extension = "." + parts[1];
            }
        }
        String fileName=UUID.randomUUID().toString().replace("-","") +extension;
        try{
            File directory=new File(uploadDirectory);
            if(!directory.exists()){
                directory.mkdir();
            }

            File outputFile=new File(uploadDirectory+fileName);
            FileOutputStream outputStream=new FileOutputStream(outputFile);
            outputStream.write(file.getBytes());
            outputStream.close();
            log.info("File saved successfully to:{} ",outputFile.getAbsolutePath());
        }catch(IOException e){
            log.info("Error saving file:{} ",e.getMessage());
        }
        return fileName;
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
    private Integer createUserInTenantDb(CreateUserRequest request, String tenantDb, String authUserId, String fileName) throws SQLException {

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
            stmt.setString(8, fileName);
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
    private Integer createRoleSpecificRecord(CreateUserRequest request, Integer userId, String tenantDb) {
        return switch (request.getRole()) {
            case "DOCTOR" -> createDoctorRecord(request.getDoctorDetails(), userId, tenantDb);
            case "NURSE" -> createNurseRecord(request.getNurseDetails(), userId, tenantDb);
            case "PATIENT" -> createPatientRecord(request.getPatientDetails(), userId, tenantDb);
            case "PHARMACIST" -> createPharmacistRecord(request.getPharmacistDetails(), userId, tenantDb);
            case "LAB_SCIENTIST" -> createLabScientistRecord(request.getLabScientistDetails(), userId, tenantDb);
            default -> {
                log.warn("No role-specific table for role: {}", request.getRole());
                yield null;
            }
        };
    }

    /**
     * Create doctor record
     */
    private Integer createDoctorRecord(CreateUserRequest.DoctorDetailsRequest details, Integer userId, String tenantDb) {

        if (details == null) {
            log.warn("Doctor details not provided for user: {}", userId);
            return null;
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

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Integer doctorId = rs.getInt("id");
                log.info("Doctor record created in tenant DB with ID: {}", doctorId);
                return doctorId;
            }
            throw new SQLException("Doctor record insertion failed — no ID returned");
        } catch (SQLException e) {
            log.error(" Failed to create doctor record", e);
            throw new RuntimeException("Doctor record creation failed: " + e.getMessage());
        }
    }
    /**
     * Create nurse record
     */
    private Integer createNurseRecord(CreateUserRequest.NurseDetailsRequest details, Integer userId, String tenantDb) {

        if (details == null) {
            log.warn("Nurse details not provided for user: {}", userId);
            return null;
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

            ResultSet rs= stmt.executeQuery();
            if (rs.next()) {
                Integer nurseId = rs.getInt("id");
                log.info("Nurse record created in tenant DB with ID: {}", nurseId);
                return nurseId;
            }
            throw new SQLException("Nurse record insertion failed — no ID returned");
            //log.info("✅ Nurse record created for user: {}", userId);

        } catch (SQLException e) {
            log.error("❌ Failed to create nurse record", e);
            throw new RuntimeException("Nurse record creation failed: " + e.getMessage());
        }
    }
    /**
     * Create patient record
     */
    private Integer createPatientRecord(CreateUserRequest.PatientDetailsRequest details, Integer userId, String tenantDb) {

        if (details == null) {
            log.warn("Patient details not provided for user: {}", userId);
            return null;
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
            ResultSet rs= stmt.executeQuery();
            if (rs.next()) {
                Integer patientId = rs.getInt("id");
                log.info("Patient record created in tenant DB with ID: {}", patientId);
                return patientId;
            }
            throw new SQLException("Patient record insertion failed — no ID returned");

        } catch (SQLException e) {
            log.error(" Failed to create patient record", e);
            throw new RuntimeException("Patient record creation failed: " + e.getMessage());
        }
    }
    private Integer createPharmacistRecord(CreateUserRequest.PharmacistDetailsRequest details,
                                        Integer userId,
                                        String tenantDb) {
        if (details == null) {
            log.warn("Pharmacist details not provided for user: {}", userId);
            return null;
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

        ResultSet rs= stmt.executeQuery();
        if (rs.next()) {
            Integer pharmacistId = rs.getInt("id");
            log.info("Create pharmacist record for user with ID: {}", pharmacistId);
            return pharmacistId;
        }
        throw new SQLException("Pharamacist record insertion failed — no ID returned");


    } catch (SQLException e) {
        log.error(" Failed to create pharmacist record"
            , e);
        throw new RuntimeException("Pharmacist record creation failed: "
        + e.getMessage());
    }
    }
    private Integer createLabScientistRecord(CreateUserRequest.LabScientistDetailsRequest details,
                                          Integer userId,
                                          String tenantDb) {
        if (details == null) {
            log.warn("LabScientist details not provided for user: {}", userId);
            return null;
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

            ResultSet rs= stmt.executeQuery();
            if (rs.next()) {
                Integer labScientistId = rs.getInt("id");
                log.info("Created labScientist record for user with ID: {}", labScientistId);
                return labScientistId;
            }
            throw new SQLException("Pharmacist record insertion failed — no ID returned");

        } catch (SQLException e) {
            log.error("❌ Failed to create lab scientist record", e);
            throw new RuntimeException("Lab scientist record creation failed: " + e.getMessage());
        }
    }
    @Transactional(rollbackFor = Exception.class)
    public UserResponse registerPatient(PatientRequest request) {

        log.info(" Patient self-registration: {}", request.getEmail());

        try {
            // Step 1: Get hospital's tenant DB name
            String tenantDb = getTenantDbNameFromHospitalId(request.getHospitalId());

            if (tenantDb == null) {
                throw new IllegalArgumentException("Hospital not found with ID: " + request.getHospitalId());
            }

            // Step 2: Check if email already exists in tenant DB
            if (emailExistsInTenantDb(request.getEmail(), tenantDb)) {
                throw new IllegalArgumentException("User with this email already exists in this hospital");
            }

            // Step 3: Register in Auth Service
            String authUserId = registerPatientInAuthService(request, request.getHospitalId(), tenantDb);

            Integer tenantUserId = createPatientUserInTenantDb(request, tenantDb, authUserId);
            //create patient in the patientDb
            String hospitalNumber = Helper.generateRandomString();
            createPatientInPatientDb(tenantUserId,tenantDb, hospitalNumber);

            log.info(" Patient registered successfully: {}", request.getEmail());

            return UserResponse.builder()
                    .success(true)
                    .message("Registration successful! Your account is pending approval.")
                    .userId(tenantUserId)
                    .authUserId(authUserId)
                    .email(request.getEmail())
                    .role("PATIENT")
                    .build();

        } catch (IllegalArgumentException e) {
            log.warn(" Registration failed: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error(" Patient registration failed", e);
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    private void createPatientInPatientDb(Integer tenantUserId, String tenantDb, String hospitalNumber) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",tenantDbHost, tenantDbPort, tenantDb);
        String sql = """
                INSERT INTO patients(
                user_id, hospital_number)
                VALUES (?, ?)
                """;
        try(Connection conn = DriverManager.getConnection(tenantUrl,tenantDbUsername,tenantDbPassword);
                                PreparedStatement statement = conn.prepareStatement(sql) ){
            statement.setInt(1,tenantUserId);
            statement.setString(2,hospitalNumber);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                log.warn("Create patient failed, no rows affected");
                throw new RuntimeException("Create patient failed, no rows affected");
            }
            log.info("Create patient successfully: {}", hospitalNumber);
        }catch (SQLException e) {
            log.error("Create patient failed", e);
            throw new RuntimeException( e.getMessage(), e);
        }
    }


    private String getTenantDbNameFromHospitalId(Integer hospitalId) {

        String onboardingUrl = String.format("jdbc:postgresql://%s:%s/onboardingdb",
                tenantDbHost, tenantDbPort);

        String sql = "SELECT db_name FROM hospital WHERE id = ? AND is_active = true";

        try (Connection conn = DriverManager.getConnection(onboardingUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, hospitalId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("db_name");
            }

            return null;

        } catch (SQLException e) {
            log.error("Error fetching hospital info", e);
            throw new RuntimeException("Failed to validate hospital");
        }
    }
    private boolean emailExistsInTenantDb(String email, String tenantDb) {

        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);

        String sql = "SELECT 1 FROM users WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            log.error("Error checking email", e);
            return false;
        }
    }
    private Integer createPatientUserInTenantDb(
            PatientRequest request,
            String tenantDb,
            String authUserId) {

        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        String sql = """
            INSERT INTO users (
                first_name, middle_name, last_name, email, phone_number,
                password, role, status, auth_user_id,
                created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """;

        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql))
        {

            stmt.setString(1, request.getFirstName());
            stmt.setString(2, request.getMiddleName());
            stmt.setString(3, request.getLastName());
            stmt.setString(4, request.getEmail());
            stmt.setString(5, request.getPhoneNumber());
            stmt.setString(6, hashedPassword);
            stmt.setString(7, "PATIENT");
            stmt.setString(8, "PENDING");  // ← Key difference: PENDING status
            stmt.setObject(9, UUID.fromString(authUserId));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Integer userId = rs.getInt("id");
                log.info("✅ Patient user created (PENDING): {}", userId);
                return userId;
            }

            throw new SQLException("Failed to create patient user");

        } catch (SQLException e) {
            log.error(" Failed to create user in tenant DB", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }
    private String registerPatientInAuthService(PatientRequest request, Integer hospitalId, String tenantDb) {

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
            log.error(" Auth service registration failed", e);
            throw new RuntimeException("Auth registration failed: " + e.getMessage());
        }
    }
    public List<HospitalListDto> getHospitalList() {
        String onboardingUrl = String.format("jdbc:postgresql://%s:%s/onboardingdb",
                tenantDbHost, tenantDbPort);
        String sql = """
            SELECT id, name, email, phone, hospital_type,\s
                   city, state, country, address
            FROM hospitals
            WHERE is_active = true
            ORDER BY name
           \s""";

        List<HospitalListDto> hospitals = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(onboardingUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                HospitalListDto hospital = HospitalListDto.builder()
                        .id(rs.getInt("id"))
                        .name(rs.getString("name"))
                        .email(rs.getString("email"))
                        .phone(rs.getString("phone"))
                        .type(rs.getString("hospital_type"))
                        .city(rs.getString("city"))
                        .state(rs.getString("state"))
                        .country(rs.getString("country"))
                        .fullAddress(buildFullAddress(
                                rs.getString("address"),
                                rs.getString("city"),
                                rs.getString("state")
                        ))
                        .build();

                hospitals.add(hospital);
            }
            log.info(" Found {} active hospitals", hospitals.size());
            return hospitals;

        } catch (SQLException e) {
            log.error(" Failed to fetch hospitals", e);
            throw new RuntimeException("Failed to fetch hospitals");
        }
    }
    private String buildFullAddress(String address, String city, String state) {
        StringBuilder fullAddress = new StringBuilder();

        if (address != null && !address.isEmpty()) {
            fullAddress.append(address);
        }
        if (city != null && !city.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(city);
        }
        if (state != null && !state.isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(state);
        }

        return fullAddress.toString();
    }

}




