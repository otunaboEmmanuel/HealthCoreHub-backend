package com.hc.hospitalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PatientDto {
    private Integer patientId;

    private String firstName;

    private String middleName;

    private String lastName;

    private String email;
    private Integer userId;

    private String phoneNumber;

    private String role;
    private String hospitalNumber;
    private LocalDateTime createdAt;

    private String status;
}
