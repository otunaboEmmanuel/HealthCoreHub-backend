package com.hc.hospitalservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    private String phoneNumber;

    @NotBlank(message = "Role is required")
    private String role;  // DOCTOR, NURSE, PATIENT, PHARMACIST, LAB_SCIENTIST, STAFF


    private DoctorDetailsRequest doctorDetails;
    private NurseDetailsRequest nurseDetails;
    private PatientDetailsRequest patientDetails;
    private PharmacistDetailsRequest pharmacistDetails;
    private LabScientistDetailsRequest labScientistDetails;

    // ✅ Static nested DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorDetailsRequest {
        private String specialization;
        private String department;
        private String licenseNumber;
        private LocalDate licenseIssueDate;
        private LocalDate licenseExpiryDate;
        private String licenseAuthority;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NurseDetailsRequest {
        private String specialization;
        private String department;
        private String licenseNumber;
        private LocalDate licenseIssueDate;
        private LocalDate licenseExpiryDate;
        private String shiftHours;
        private Integer yearsOfExperience;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientDetailsRequest {
        private String patientId;
        private LocalDate dateOfBirth;
        private String hospitalNumber;
        private String gender;
        private String bloodGroup;
        private String genotype;
        private String maritalStatus;
        private String occupation;
        private String country;
        private String state;
        private String city;
        private String address;
        private String nextOfKinName;
        private String nextOfKinRelationship;
        private String nextOfKinPhone;
        private String emergencyContactName;
        private String emergencyContactPhone;
        private String allergies;
        private String chronicConditions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PharmacistDetailsRequest {
        private String licenseNumber;
        private LocalDate licenseIssueDate;
        private LocalDate licenseExpiryDate;
        private String specialization;
        private String department;
        private Integer yearsOfExperience;
        private String licenseAuthority;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabScientistDetailsRequest {
        private String licenseNumber;
        private LocalDate licenseIssueDate;
        private LocalDate licenseExpiryDate;
        private String specialization;
        private String department;
        private String licenseAuthority;
        private Integer yearsOfExperience;
    }
}
