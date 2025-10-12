package com.hc.onboardingservice.service;

import com.hc.onboardingservice.requests.HospitalRegistrationRequest;
import com.hc.onboardingservice.dto.HospitalRegistrationResponse;
import com.hc.onboardingservice.entity.Hospital;
import com.hc.onboardingservice.entity.HospitalAdmin;
import com.hc.onboardingservice.entity.Plan;
import com.hc.onboardingservice.repository.HospitalAdminRepository;
import com.hc.onboardingservice.repository.HospitalRepository;
import com.hc.onboardingservice.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final HospitalAdminRepository adminRepository;
    private final PlanRepository planRepository;
    private final TenantDatabaseService tenantDatabaseService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional(rollbackFor = Exception.class)
    public HospitalRegistrationResponse registerHospital(HospitalRegistrationRequest request) {

        log.info("🏥 Starting hospital registration for: {}", request.getHospital().getName());

        // 1. Validate hospital doesn't already exist
        validateHospitalDoesNotExist(request.getHospital().getEmail(), request.getAdmin().getEmail());

        // 2. Get plan
        Plan plan = planRepository.findById(request.getHospital().getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + request.getHospital().getPlanId()));

        // 3. Generate tenant database credentials
        String dbName = generateDbName(request.getHospital().getName());
        String dbUser = generateDbUser(dbName);
        String dbPassword = generateSecurePassword();

        // 4. Create hospital record in master database
        Hospital hospital = createHospitalRecord(request, plan, dbName, dbUser, dbPassword);

        try {
            // 5. Create tenant database
            log.info("📦 Creating tenant database: {}", dbName);
            tenantDatabaseService.createTenantDatabase(dbName, dbUser, dbPassword);

            // 6. Initialize tenant schema (run migrations)
            log.info("🔧 Initializing tenant schema");
            tenantDatabaseService.initializeTenantSchema(dbName, dbUser, dbPassword);

            // 7. Create admin user in tenant database
            log.info("👤 Creating admin user in tenant database");
            Integer tenantUserId = createAdminInTenantDb(request.getAdmin(), dbName, dbUser, dbPassword);

            // 8. Create admin reference in master database
            log.info("📝 Creating admin reference in master database");
            HospitalAdmin admin = createAdminInMasterDb(request.getAdmin(), hospital, tenantUserId);

            // 9. Activate hospital (since no payment for now)
            log.info("✅ Activating hospital");
            hospital.setIsActive(true);
            hospitalRepository.save(hospital);

            // 10. Send confirmation email
            log.info("📧 Sending confirmation email");
            sendConfirmationEmail(hospital, admin, request.getAdmin().getPassword());

            // 11. Build response
            HospitalRegistrationResponse response = buildSuccessResponse(hospital, admin, plan);

            log.info("🎉 Hospital registration completed successfully for: {}", hospital.getName());
            return response;

        } catch (Exception ex) {
            log.error("❌ Error during hospital registration, rolling back...", ex);

            // Cleanup: Drop the tenant database if it was created
            try {
                tenantDatabaseService.dropTenantDatabase(dbName, dbUser);
                log.info("🧹 Cleaned up tenant database after failure");
            } catch (Exception cleanupEx) {
                log.warn("⚠️ Could not cleanup tenant database", cleanupEx);
            }

            throw new RuntimeException("Hospital registration failed: " + ex.getMessage(), ex);
        }
    }

    private void validateHospitalDoesNotExist(String hospitalEmail, String adminEmail) {
        if (hospitalRepository.findByEmail(hospitalEmail).isPresent()) {
            throw new IllegalArgumentException("Hospital with email '" + hospitalEmail + "' already exists");
        }
        if (adminRepository.existsByEmail(adminEmail)) {
            throw new IllegalArgumentException("Admin with email '" + adminEmail + "' already exists");
        }
    }

    private String generateDbName(String hospitalName) {
        // Convert hospital name to valid database name
        // "Grace Specialist Hospital" -> "tenant_grace_specialist_hospital"
        String sanitized = hospitalName.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")  // Remove special characters
                .replaceAll("\\s+", "_")          // Replace spaces with underscores
                .substring(0, Math.min(40, hospitalName.length()));  // Limit length

        return "tenant_" + sanitized;
    }

    private String generateDbUser(String dbName) {
        return dbName + "_user";
    }

    private String generateSecurePassword() {
        // Generate a secure random password
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private Hospital createHospitalRecord(HospitalRegistrationRequest request,
                                          Plan plan,
                                          String dbName,
                                          String dbUser,
                                          String dbPassword) {

        Hospital hospital = Hospital.builder()
                .name(request.getHospital().getName())
                .email(request.getHospital().getEmail())
                .phone(request.getHospital().getPhone())
                .hospitalType(request.getHospital().getType())
                .country(request.getHospital().getCountry())
                .state(request.getHospital().getState())
                .city(request.getHospital().getCity())
                .address(request.getHospital().getAddress())
                .plan(plan)
                .dbName(dbName)
                .dbUser(dbUser)
                .dbPassword(passwordEncoder.encode(dbPassword))  // In production, encrypt this!
                .isActive(false)
                .createdAt(LocalDateTime.now())
                .build();

        return hospitalRepository.save(hospital);
    }

    private Integer createAdminInTenantDb(HospitalRegistrationRequest.AdminInfo adminInfo,
                                          String dbName,
                                          String dbUser,
                                          String dbPassword) {

        String tenantUrl = "jdbc:postgresql://localhost:5433/" + dbName;
        String hashedPassword = passwordEncoder.encode(adminInfo.getPassword());

        String insertSql = """
            INSERT INTO users (first_name, last_name, email, phone_number, password, role, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = DriverManager.getConnection(tenantUrl, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {

            stmt.setString(1, adminInfo.getFirstName());
            stmt.setString(2, adminInfo.getLastName());
            stmt.setString(3, adminInfo.getEmail());
            stmt.setString(4, adminInfo.getPhone());
            stmt.setString(5, hashedPassword);
            stmt.setString(6, "ADMIN");
            stmt.setString(7, "ACTIVE");

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Integer userId = rs.getInt("id");
                log.info("✅ Created admin user in tenant DB with ID: {}", userId);
                return userId;
            }

            throw new SQLException("Failed to create admin user in tenant database");

        } catch (SQLException e) {
            log.error("❌ Error creating admin in tenant database", e);
            throw new RuntimeException("Failed to create admin user: " + e.getMessage(), e);
        }
    }

    private HospitalAdmin createAdminInMasterDb(HospitalRegistrationRequest.AdminInfo adminInfo,
                                                Hospital hospital,
                                                Integer tenantUserId) {

        HospitalAdmin admin = HospitalAdmin.builder()
                .hospital(hospital)
                .firstName(adminInfo.getFirstName())
                .lastName(adminInfo.getLastName())
                .email(adminInfo.getEmail())
                .phone(adminInfo.getPhone())
                .passwordHash(passwordEncoder.encode(adminInfo.getPassword()))
                .role("ADMIN")
                .tenantUserId(tenantUserId)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return adminRepository.save(admin);
    }

    private void sendConfirmationEmail(Hospital hospital, HospitalAdmin admin,String rawPassword) {
        try {
            emailService.sendHospitalActivationEmail(
                    admin.getEmail(),
                    admin.getFirstName() + " " + admin.getLastName(),
                    hospital.getName(),
                    hospital.getDbName(),
                    rawPassword
            );
        } catch (Exception e) {
            log.warn("⚠️ Failed to send confirmation email", e);
            // Don't fail the registration if email fails
        }
    }

    private HospitalRegistrationResponse buildSuccessResponse(Hospital hospital,
                                                              HospitalAdmin admin,
                                                              Plan plan) {
        return HospitalRegistrationResponse.builder()
                .code("00")
                .message("Hospital registered successfully! Confirmation email sent.")
                .hospital(HospitalRegistrationResponse.HospitalData.builder()
                        .id(hospital.getId())
                        .name(hospital.getName())
                        .email(hospital.getEmail())
                        .phone(hospital.getPhone())
                        .type(hospital.getHospitalType())
                        .dbName(hospital.getDbName())
                        .planName(plan.getName())
                        .planDurationDays(plan.getDurationDays())
                        .isActive(hospital.getIsActive())
                        .createdAt(hospital.getCreatedAt())
                        .build())
                .admin(HospitalRegistrationResponse.AdminData.builder()
                        .id(admin.getId())
                        .firstName(admin.getFirstName())
                        .lastName(admin.getLastName())
                        .email(admin.getEmail())
                        .phone(admin.getPhone())
                        .role(admin.getRole())
                        .createdAt(admin.getCreatedAt())
                        .build())
                .build();
    }

    public Hospital findHospital(Integer id) {
        return hospitalRepository.findById(id).orElse(null);
    }

    public Hospital deleteHospital(Integer id) {
        hospitalRepository.findById(id).ifPresent(hospitalRepository::delete);
        return null;
    }

//    public Hospital updateHospital(Integer id, HospitalRegistrationRequest updateRequest) {
//        Hospital hospital = hospitalRepository.findById(id).orElse(null);
//        if (hospital == null) {
//            return  null;
//        }
//        hospital.setHospitalType(updateRequest.getHospital().getType());
//        hospital.setName(updateRequest.getHospital().getName());
//        hospital.setEmail(updateRequest.getHospital().getEmail());
//        hospital.setPhone(updateRequest.getHospital().getPhone());
//        hospital.setCity(updateRequest.getHospital().getCity());
//        hospital.setAddress(updateRequest.getHospital().getAddress());
//        hospital.setAdmins(updateRequest.getAdmin().);
//    }
}