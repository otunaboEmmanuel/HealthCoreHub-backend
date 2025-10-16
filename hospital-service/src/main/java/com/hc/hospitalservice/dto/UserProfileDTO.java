package com.hc.hospitalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Integer id;

    private String firstName;

    private String middleName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private String role;

    private String profilePicture;

    private String status;

    /**
     * This field holds role-specific details.
     * It can be an instance of:
     * - DoctorDetailsResponse
     * - NurseDetailsResponse
     * - PatientDetailsResponse
     * - PharmacistDetailsResponse
     * - LabScientistDetailsResponse
     */
    private Object roleDetails;

}
