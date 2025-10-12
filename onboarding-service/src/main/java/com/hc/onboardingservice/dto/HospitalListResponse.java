package com.hc.onboardingservice.dto;

import com.hc.onboardingservice.entity.Hospital;
import com.hc.onboardingservice.entity.HospitalAdmin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalListResponse {
    private Integer id;
    private String name;
    private String email;
    private String phone;
    private String hospitalType;
    private String country;
    private String state;
    private String city;
    private String address;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private PlanInfo plan;
    private List<AdminInfo> admins;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanInfo {
        private Integer id;
        private String name;
        private String description;
        private Double price;
        private Integer durationDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminInfo {
        private Integer id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String role;
        private Boolean isActive;
        private LocalDateTime createdAt;
        // NO PASSWORD FIELD - it's excluded
    }

    // Constructor to map from Hospital entity
    public HospitalListResponse(Hospital hospital) {
        this.id = hospital.getId();
        this.name = hospital.getName();
        this.email = hospital.getEmail();
        this.phone = hospital.getPhone();
        this.hospitalType = hospital.getHospitalType();
        this.country = hospital.getCountry();
        this.state = hospital.getState();
        this.city = hospital.getCity();
        this.address = hospital.getAddress();
        this.isActive = hospital.getIsActive();
        this.createdAt = hospital.getCreatedAt();

        // Map plan
        if (hospital.getPlan() != null) {
            this.plan = PlanInfo.builder()
                    .id(hospital.getPlan().getId())
                    .name(hospital.getPlan().getName())
                    .description(hospital.getPlan().getDescription())
                    .price(hospital.getPlan().getPrice())
                    .durationDays(hospital.getPlan().getDurationDays())
                    .build();
        }

        // Map admins (excluding password)
        if (hospital.getAdmins() != null) {
            this.admins = hospital.getAdmins().stream()
                    .map(admin -> AdminInfo.builder()
                            .id(admin.getId())
                            .firstName(admin.getFirstName())
                            .lastName(admin.getLastName())
                            .email(admin.getEmail())
                            .phone(admin.getPhone())
                            .role(admin.getRole())
                            .isActive(admin.getIsActive())
                            .createdAt(admin.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
        }
    }
}