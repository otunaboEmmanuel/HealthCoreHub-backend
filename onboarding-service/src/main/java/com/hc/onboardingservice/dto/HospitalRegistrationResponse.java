package com.hc.onboardingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalRegistrationResponse {
    private String code;
    private String message;
    private HospitalData hospital;
    private AdminData admin;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HospitalData {
        private Integer id;
        private String name;
        private String email;
        private String phone;
        private String type;
        private String dbName;
        private String planName;
        private Integer planDurationDays;
        private Boolean isActive;
        private String subscriptionStatus;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminData {
        private Integer id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String role;
        private LocalDateTime createdAt;
    }
}