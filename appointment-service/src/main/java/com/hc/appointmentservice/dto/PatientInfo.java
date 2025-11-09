package com.hc.appointmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PatientInfo {
    private String firstName;
    private String lastName;
    private String email;
    private String profile_picture;
}
