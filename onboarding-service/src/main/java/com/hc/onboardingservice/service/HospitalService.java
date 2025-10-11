package com.hc.onboardingservice.service;

import com.hc.onboardingservice.entity.Plan;
import com.hc.onboardingservice.repository.PlanRepository;
import com.hc.onboardingservice.requests.HospitalRequest;
import com.hc.onboardingservice.requests.UpdateRequest;
import com.hc.onboardingservice.entity.Hospital;
import com.hc.onboardingservice.repository.HospitalRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalService {
    private final HospitalRepository hospitalRepository;
    private final PlanRepository planRepository;
    private final TenantDatabaseService tenantDatabaseService;

    @Transactional
    public Hospital registerHospital(HospitalRequest hospitalRequest) {
        // 1. Find the selected plan
        Plan plan = planRepository.findById(hospitalRequest.getPlan().getId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        // 2. Check if hospital already exists
        if (hospitalRepository.findByName(hospitalRequest.getName()).isPresent()) {
            throw new IllegalArgumentException("Hospital with this name already exists");
        }

        // 3. Generate unique DB name, user, and password
        String dbName = "tenant_" + hospitalRequest.getName().toLowerCase().replaceAll("\\s+", "_");
        String dbUser = dbName + "_user";
        String dbPassword = UUID.randomUUID().toString().substring(0, 12);// random strong password

        Optional<Hospital> existingHospital = hospitalRepository.findByName(hospitalRequest.getName());
        Optional<Hospital> existingDb = hospitalRepository.findByDbName(dbName);

        if (existingHospital.isPresent()) {
            throw new IllegalArgumentException("Hospital with this name already exists");
        }

        if (existingDb.isPresent()) {
            throw new IllegalArgumentException("Database for this hospital already exists");
        }

        // 4. Create hospital record (PENDING until tenant DB setup completes)
        Hospital hospital = Hospital.builder()
                .name(hospitalRequest.getName())
                .email(hospitalRequest.getEmail())
                .phone(hospitalRequest.getPhoneNumber())
                .hospitalType(hospitalRequest.getHospitalType())
                .country(hospitalRequest.getCountry())
                .state(hospitalRequest.getState())
                .city(hospitalRequest.getCity())
                .address(hospitalRequest.getAddress())
                .plan(plan)
                .dbName(dbName)
                .dbUser(dbUser)
                .dbPassword(dbPassword)
                .isActive(false)
                .createdAt(LocalDateTime.now())
                .build();

        Hospital savedHospital = hospitalRepository.save(hospital);

        try {
            // 5. Create tenant DB and run migrations
            tenantDatabaseService.createTenantDatabase(dbName, dbUser, dbPassword);
            tenantDatabaseService.initializeTenantSchema(dbName, dbUser, dbPassword);

            // 6. Activate hospital
            savedHospital.setIsActive(true);
            savedHospital = hospitalRepository.save(savedHospital);

            log.info("✅ Hospital '{}' successfully onboarded with DB '{}'", savedHospital.getName(), dbName);

        } catch (Exception ex) {
            log.error("❌ Error creating tenant DB for hospital '{}': {}", hospitalRequest.getName(), ex.getMessage());
            throw new RuntimeException("Failed to create tenant database: " + ex.getMessage());
        }

        return savedHospital;
    }

//    public Hospital updateHospital(Integer id, UpdateRequest updateRequest) {
//        Hospital hospital= hospitalRepository.findById(id).orElse(null);
//        if (hospital == null)
//        {
//            return null;
//        }
//        hospital.setName(updateRequest.getName());
//        hospital.setEmail(updateRequest.getEmail());
//        hospital.setContactPerson(updateRequest.getContactPerson());
//        hospital.setAddress(updateRequest.getAddress());
//        hospital.setPhoneNumber(updateRequest.getPhoneNumber());
//        hospital.setCreatedAt(LocalDateTime.now());
//        hospital.setStatus(updateRequest.getStatus());
//        return hospitalRepository.save(hospital);
//    }

    public Hospital findHospital(Integer id) {
        return hospitalRepository.findById(id).orElse(null);
    }

    public Hospital deleteHospital(Integer id) {
        Hospital hospital = hospitalRepository.findById(id).orElse(null);
        if (hospital != null)
        {
            hospitalRepository.delete(hospital);
        }
        return hospital;
    }
}
