package com.hc.onboardingservice.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalRegistrationRequest {

    @Valid
    @NotNull(message = "Hospital information is required")
    private HospitalInfo hospital;

    @Valid
    @NotNull(message = "Admin information is required")
    private AdminInfo admin;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HospitalInfo {
        @NotBlank(message = "Hospital name is required")
        @Size(max = 200)
        private String name;

        @NotBlank(message = "Hospital email is required")
        @Email(message = "Invalid email format")
        @Size(max = 150)
        private String email;

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
        private String phone;

        @NotBlank(message = "Hospital type is required")
        private String type;  // clinic, general_hospital, specialist_hospital

        @NotBlank(message = "Country is required")
        private String country;

        @NotBlank(message = "State is required")
        private String state;

        @NotBlank(message = "City is required")
        private String city;

        @NotBlank(message = "Address is required")
        private String address;

        @NotNull(message = "Plan ID is required")
        @Min(value = 1, message = "Invalid plan ID")
        private Integer planId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminInfo {
        @NotBlank(message = "First name is required")
        @Size(max = 150)
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 150)
        private String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 150)
        private String email;

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
        private String phone;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
    }
}