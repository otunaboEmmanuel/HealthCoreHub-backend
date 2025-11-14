package com.hc.hospitalservice.service;

import com.hc.hospitalservice.dto.*;
import com.hc.hospitalservice.grpc.AuthServiceGrpcClient;
import com.hc.hospitalservice.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final EmailService emailService;
    private final UserProfileService userProfileService;
    private final AuthServiceGrpcClient  authServiceGrpcClient;
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
    @Value("${file.upload.directory:/app/uploads}/")
    private String uploadDirectory;
    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;



    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createUser(CreateUserRequest request, String tenantDb, Integer hospitalId) {


        log.info("👤 Creating user: {} with role: {}", request.getEmail(), request.getRole());

        String userIdStr=null;
        try {
            // Step 1: Register in Auth Service
            Map<String, String> grpcInfo = registerInAuthService(request, hospitalId, tenantDb);
            userIdStr = grpcInfo.get("userId");
            if (userIdStr == null || userIdStr.equals("null")) {
                throw new RuntimeException("gRPC did not return a valid userId");
            }
            String activationToken = grpcInfo.get("activationCode");
            if (activationToken == null || activationToken.equals("null")) {
                throw new RuntimeException("gRPC did not return a valid activationToken");
            }

            //CREATING ACTIVATION LINK
            String activationLink = String.format("%s/activate?token=%s",
                    frontendUrl, activationToken);
            // Step 2: Create user in Tenant DB

            Integer tenantUserId = createUserInTenantDb(request, tenantDb, userIdStr);
            if (tenantUserId == null) {
                log.info("deleting user with id {}", userIdStr);
                authServiceGrpcClient.deleteUser(userIdStr);
            }
            Integer staffId = null;
            if (shouldCreateRoleSpecificRecord(request.getRole())) {
                staffId = createRoleSpecificRecord(request, tenantUserId, tenantDb);
            }
            String hospitalName = userProfileService.getHospitalNameFromTenantDb(tenantDb);
            emailService.sendActivationEmail(request.getEmail(), hospitalName, activationToken, activationLink);
            log.info(" User created successfully: {}", request.getEmail());
            Map<String, Object> response = new HashMap<>();
            response.put("userId", tenantUserId);
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("staffId", staffId);
            response.put("email", request.getEmail());
            response.put("role", request.getRole());
            response.put("authUserId", userIdStr);
            return  response;

        } catch (Exception e) {
            log.error("Failed to create user: {}", request.getEmail(), e);
            if (userIdStr != null) {
                try {
                    authServiceGrpcClient.deleteUser(userIdStr);
                } catch (Exception ex) {
                    log.error("Failed to rollback auth-service user: {}", userIdStr, ex);
                }
            }
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
                directory.mkdirs();
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

    private Map<String, String> registerInAuthService(CreateUserRequest request, Integer hospitalId, String tenantDb) {

        log.info(" Creating user: {} with role: {}", request.getEmail(), request.getRole());
        try{
            log.info("creating user via grpc ");
            return authServiceGrpcClient.registerStaff(request.getEmail(),
                    hospitalId,tenantDb,"HOSPITAL_USER");
        }catch(Exception e){
            log.info("Error creating user via grpc ");
            throw new RuntimeException("Auth service registration failed: " + e.getMessage());
        }
    }
    /**
     * Create user in tenant database
     */
    private Integer createUserInTenantDb(CreateUserRequest request, String tenantDb, String authUserId) throws SQLException {

        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);
        //String hashedPassword = passwordEncoder.encode(request.getPassword());

        String sql = """
            INSERT INTO users (
                first_name, middle_name, last_name, email, phone_number,password,
                role, status, auth_user_id,
                created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?,?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """;

        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, request.getFirstName());
            stmt.setString(2, request.getMiddleName());
            stmt.setString(3, request.getLastName());
            stmt.setString(4, request.getEmail());
            stmt.setString(5, request.getPhoneNumber());
            stmt.setString(6,"");
            stmt.setString(7, request.getRole());
            stmt.setString(8, "PENDING");
            stmt.setObject(9, UUID.fromString(authUserId));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Integer userId = rs.getInt("id");
                log.info("✅ User created in tenant DB with ID: {}", userId);
                return userId;
            }

            return null;

        } catch (SQLException e) {
            log.error(" Tenant DB user creation failed", e);
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
            RETURNING id
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

            boolean hasResultSet = stmt.execute();
            if (hasResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        Integer doctorId = rs.getInt("id");
                        log.info(" Doctor record created in tenant DB with ID: {}", doctorId);
                        return doctorId;
                    }
                }
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

            boolean hasResultSet = stmt.execute();
            if (hasResultSet) {
                try(ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        Integer nurseId = rs.getInt("id");
                        log.info("Nurse record created in tenant DB with ID: {}", nurseId);
                        return nurseId;
                    }
                }
            }
            throw new SQLException("Nurse record insertion failed — no ID returned");
        } catch (SQLException e) {
            log.error(" Failed to create nurse record", e);
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
            boolean hasResultSet = stmt.execute();
            if (hasResultSet) {
                try(ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        Integer patientId = rs.getInt("id");
                        log.info("Patient record created in tenant DB with ID: {}", patientId);
                        return patientId;
                    }
                }
            }
            throw new SQLException("PatientId record insertion failed — no ID returned");
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
        boolean hasResultSet = stmt.execute();
        if (hasResultSet) {
            try (ResultSet rs = stmt.getResultSet()) {
                if (rs.next()) {
                    Integer pharmacistId = rs.getInt("id");
                    log.info("Create pharmacist record for user with ID: {}", pharmacistId);
                    return pharmacistId;
                }
            }
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

          boolean hasResultSet = stmt.execute();
          if (hasResultSet) {
              try (ResultSet rs = stmt.getResultSet()) {
                  if (rs.next()) {
                      Integer labScientistId = rs.getInt("id");
                      log.info("lab scientist record created in user with ID: {}", labScientistId);
                      return labScientistId;
                  }
              }
          }
          throw new SQLException("error getting lab scientist record");

        } catch (SQLException e) {
            log.error(" Failed to create lab scientist record", e);
            throw new RuntimeException("Lab scientist record creation failed: " + e.getMessage());
        }
    }
    @Transactional(rollbackFor = Exception.class)
    public UserResponse registerPatient(PatientRequest request) {

        log.info("👤 Patient self-registration: {}", request.getEmail());

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

            // Step 4: Create user in tenant DB
            Integer tenantUserId = createPatientUserInTenantDb(request, tenantDb, authUserId);

            // Step 5: Create patient with unique hospital number (with retry)
            Integer patientId = createPatientWithUniqueHospitalNumber(tenantUserId, tenantDb);

            // Get the actual hospital number that was used
            String hospitalNumber = getPatientHospitalNumber(patientId, tenantDb);

            log.info(" Patient registered successfully: {} with hospital number: {}",
                    request.getEmail(), hospitalNumber);

            return UserResponse.builder()
                    .success(true)
                    .message("Registration successful! Your account is pending approval.")
                    .userId(tenantUserId)
                    .authUserId(authUserId)
                    .hospitalNumber(hospitalNumber)
                    .patientId(patientId)
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

    private Integer createPatientWithUniqueHospitalNumber(Integer tenantUserId, String tenantDb) {
        int maxRetries = 5;
        SQLException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String hospitalNumber = Helper.generateRandomString();

            log.info(" Creating patient with hospital number: {} (attempt {}/{})",
                    hospitalNumber, attempt, maxRetries);

            try {
                return createPatientInPatientDbNoThrow(tenantUserId, tenantDb, hospitalNumber);

            } catch (SQLException e) {
                lastException = e;

                // Check if it's a duplicate key error
                if (e.getMessage().contains("duplicate key") &&
                        e.getMessage().contains("patients_hospital_number_key")) {

                    log.warn("⚠️ Duplicate hospital number: {}. Retrying... (attempt {}/{})",
                            hospitalNumber, attempt, maxRetries);

                    if (attempt < maxRetries) {
                        continue; // Try again
                    }
                } else {
                    // Different SQL error, don't retry
                    log.error(" SQL error creating patient", e);
                    throw new RuntimeException("Database error while creating patient", e);
                }
            }
        }

        log.error(" Failed to create patient after {} attempts", maxRetries);
        throw new RuntimeException(
                "Failed to generate unique hospital number after " + maxRetries + " attempts",
                lastException);
    }

    // Version that throws SQLException directly
    private Integer createPatientInPatientDbNoThrow(Integer tenantUserId, String tenantDb,
                                                    String hospitalNumber) throws SQLException {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);

        String sql = """
            INSERT INTO patients (user_id, hospital_number, created_at)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            RETURNING id
            """;

        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, tenantUserId);
            stmt.setString(2, hospitalNumber);
            boolean result = stmt.execute();
            if(result) {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        Integer patientId = rs.getInt("id");
                        log.info("✅ Patient created with ID: {} and hospital number: {}",
                                patientId, hospitalNumber);
                        return patientId;
                    }
                }
            }
            throw new SQLException("Failed to create patient record - no ID returned");
        }
    }

    private String getPatientHospitalNumber(Integer patientId, String tenantDb) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);

        String sql = "SELECT hospital_number FROM patients WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, patientId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("hospital_number");
                }
                throw new RuntimeException("Patient record not found after creation");
            }

        } catch (SQLException e) {
            log.error(" Error fetching patient hospital number", e);
            throw new RuntimeException("Failed to fetch hospital number", e);
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
                log.info(" Patient user created (PENDING): {}", userId);
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
            log.info(" User registered in auth service: {}", authUserId);
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

    public Map<String, Object> getHospitalNumber(String tenantDb, Integer patientId) {
        log.info("Getting hospital number from tenant DB: {}", tenantDb);
        String hospitalNumber = getHospitalNumberFromTenantDb(tenantDb,patientId);
        Map<String, Object> hospitalNumberMap = new HashMap<>();
        hospitalNumberMap.put("hospitalNumber", hospitalNumber);
        return hospitalNumberMap;
    }

    private String getHospitalNumberFromTenantDb(String tenantDb, Integer patientId)  {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);
        String sql = """
              SELECT hospital_number
              FROM patients
              WHERE patient_id = ?
              """;
        try( Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
                PreparedStatement statement = conn.prepareStatement(sql)){
                    ResultSet rs = statement.executeQuery();
                    if(rs.next()){
                        return rs.getString("hospital_number");
                    }
                    return null;
        }catch (SQLException e){
                    log.error(" Failed to fetch hospital number", e);
                    throw new RuntimeException("Failed to fetch hospital number");
        }
    }

    public List<PatientDto> getPatients(String tenantDb) {
        String tenantUrl = String.format(
                "jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb
        );

        List<PatientDto> patients = new ArrayList<>();

        String sql = """
        SELECT 
            p.id,
            u.first_name,
            u.middle_name,
            u.last_name,
            u.email,
            u.phone_number,
            u.role,
            p.user_id,
            u.created_at,
            u.status,
            p.hospital_number
        FROM patients p
        INNER JOIN users u ON p.user_id = u.id
        WHERE u.status = ?
        """;

        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "ACTIVE");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    patients.add(PatientDto.builder()
                            .patientId(rs.getInt("id"))
                            .userId(rs.getInt("user_id"))
                            .firstName(rs.getString("first_name"))
                            .middleName(rs.getString("middle_name"))
                            .lastName(rs.getString("last_name"))
                            .email(rs.getString("email"))
                            .createdAt(rs.getObject("created_at", LocalDateTime.class))
                            .phoneNumber(rs.getString("phone_number"))
                            .role(rs.getString("role"))
                            .status(rs.getString("status"))
                            .hospitalNumber(rs.getString("hospital_number"))
                            .build());
                }
            }

        } catch (SQLException e) {
            log.error("Error fetching patients from tenant DB {}: {}", tenantDb, e.getMessage(), e);
            throw new RuntimeException("Database error while fetching patients", e);
        }

        return patients;
    }

    public Map<String, Object> uploadProfilePicture(Integer userId, MultipartFile file, String tenantDb) {
        log.info("uploding profile picture from tenant DB: {}", tenantDb);
        String sql = "SELECT 1 FROM users WHERE id = ?";
        if(!idExists(userId, tenantDb,sql)){
            log.info("user id not exists in tenant DB: {}", tenantDb);
            throw new RuntimeException("user id not exists in tenant DB");
        }
        String profile_picture = saveFileToStorage(file);
        updateUserTenantDb(profile_picture, userId, tenantDb);
        Map<String, Object> result = new HashMap<>();
        result.put("profile_picture", profile_picture);
        return result;
    }

    private void updateUserTenantDb(String profilePicture, Integer userId, String tenantDb) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
        String sql = """
            UPDATE users
            SET profile_picture = ?
            WHERE id = ?
       """;
        try(Connection conn = DriverManager.getConnection(tenantUrl,tenantDbUsername,tenantDbPassword);
                                        PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, profilePicture);
            statement.setInt(2, userId);
            int affectedRows = statement.executeUpdate();
            if (affectedRows > 0) {
                log.info(" User {} profile picture updated successfully", userId);
            } else {
                log.warn(" User with id {} does not exist in tenant {}", userId, tenantDb);
                throw new RuntimeException("User with id " + userId + " does not exist");
            }

        }catch (SQLException e){
            log.error(" Failed to fetch user profile picture", e);
            throw new RuntimeException("Failed to fetch user profile picture");
        }
    }

    public String getProfilePicture(String tenantDb, Integer userId) {
        String sql = "SELECT 1 FROM users WHERE id = ?";
        if(!idExists(userId,tenantDb,sql)){
            log.warn("User with id {} does not exist in tenant {}", userId, tenantDb);
            throw new RuntimeException("User with id " + userId + " does not exist");
        }
        return getProfilePictureFromTenantDb(tenantDb, userId);
    }

    private String getProfilePictureFromTenantDb(String tenantDb, Integer userId) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
        String sql = """
            SELECT profile_picture
            FROM users WHERE id = ?
        """;
        try(Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername,tenantDbPassword);
                                PreparedStatement statement = conn.prepareStatement(sql) ){
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            if(rs.next()){
                return rs.getString("profile_picture");
            }
            throw new RuntimeException("User with id " + userId + " does not exist in tenant DB");
        }catch (SQLException e){
            log.error(" Failed to fetch user profile picture", e);
            throw new RuntimeException("Failed to fetch user profile picture");
        }
    }

    private boolean idExists(Integer id, String tenantDb, String sql) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);
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
    public ResponseEntity<byte[]> getProfilePictureResponse(String filePath) {
        File file = new File(uploadDirectory + filePath);

        if (!file.exists() || !file.isFile()) {
            log.warn("Profile picture not found at path: {}", file.getAbsolutePath());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }

        try {
            byte[] imageData = Files.readAllBytes(file.toPath());
            String contentType = Files.probeContentType(file.toPath());

            log.info("Serving profile picture: {} [{} bytes]", file.getName(), imageData.length);

            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.valueOf(contentType != null ? contentType : "application/octet-stream"))
                    .body(imageData);

        } catch (IOException e) {
            log.error("Error reading profile picture: {}", file.getAbsolutePath(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}




