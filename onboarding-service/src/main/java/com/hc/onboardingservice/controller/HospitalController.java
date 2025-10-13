package com.hc.onboardingservice.controller;

import com.hc.onboardingservice.dto.HospitalListResponse;
import com.hc.onboardingservice.dto.HospitalRegistrationResponse;
import com.hc.onboardingservice.entity.Hospital;
import com.hc.onboardingservice.requests.HospitalRegistrationRequest;

import com.hc.onboardingservice.repository.HospitalRepository;
import com.hc.onboardingservice.service.HospitalService;
import com.hc.onboardingservice.service.TenantDatabaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/hospital")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class HospitalController {
    private final HospitalService hospitalService;
    private final HospitalRepository hospitalRepository;
    private final TenantDatabaseService tenantDatabaseService;
    @PostMapping
    public ResponseEntity<HospitalRegistrationResponse> registerHospital(
            @Valid @RequestBody HospitalRegistrationRequest request) {

        log.info("📝 Received hospital registration request for: {}",
                request.getHospital().getName());

        try {
            HospitalRegistrationResponse response = hospitalService.registerHospital(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    HospitalRegistrationResponse.builder()
                            .code("01")
                            .message(e.getMessage())
                            .build()
            );

        } catch (Exception e) {
            log.error("❌ Registration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    HospitalRegistrationResponse.builder()
                            .code("99")
                            .message("Registration failed: " + e.getMessage())
                            .build()
            );
        }
    }
    @GetMapping()
    public ResponseEntity<Page<?>> getRegisteredHospitals(@RequestParam(name = "page",defaultValue = "0", required = false)int page,
                                          @RequestParam(name= "size", defaultValue = "10", required = false) int size)
    {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Hospital> hospitals = hospitalRepository.findAllWithDetails(pageable);
        Page<HospitalListResponse> response = hospitals.map(HospitalListResponse::new);
        return ResponseEntity.ok(response);
    }
    @GetMapping("{id}")
    public ResponseEntity<?> findHospital(@PathVariable Integer id)
    {
        Map<String, String> response = new HashMap<>();
        Hospital hospital = hospitalService.findHospital(id);
        if (hospital == null)
        {
            response.put("code", "101");
            response.put("message", "hospital does not exist");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        return new ResponseEntity<>(hospital, HttpStatus.OK);
    }
//    @PutMapping("{id}")
//    public ResponseEntity<?>updateHospital(@PathVariable Integer id, @RequestBody HospitalRegistrationRequest updateRequest)
//        {
//            Map<String, String> response = new HashMap<>();
//            Hospital hospital = hospitalService.updateHospital(id, updateRequest);
//            if (hospital == null)
//            {
//                response.put("code", "100");
//                response.put("message", "hospital does not exist".toUpperCase());
//            }
//                response.put("code", "00");
//                response.put("message", "hospital successfully updated".toUpperCase());
//             return new ResponseEntity<>(response, HttpStatus.OK);
//        }

    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteHospital(@PathVariable Integer id)
    {
        Map<String, String> response = new HashMap<>();
        Hospital hospital = hospitalService.deleteHospital(id);
        if (hospital == null)
        {
            response.put("code", "101");
            response.put("message", "hospital does not exist".toUpperCase());
        }
        response.put("code", "00");
        response.put("message", "hospital successfully deleted".toUpperCase());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
//    @DeleteMapping("/{dbName}")
//    public ResponseEntity<?> dropTenantDatabase(
//            @PathVariable String dbName,
//            @RequestParam String dbUser) {
//
//        try {
//            tenantDatabaseService.dropTenantDatabase(dbName, dbUser);
//            return ResponseEntity.ok(Map.of(
//                    "code", "00",
//                    "message", "Tenant database dropped successfully"
//            ));
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of(
//                    "code", "99",
//                    "message", "Failed to drop database: " + e.getMessage()
//            ));
//        }
//    }
}
