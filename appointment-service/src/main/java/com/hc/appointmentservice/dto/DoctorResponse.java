package com.hc.appointmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DoctorResponse {
    private String firstName;
    private String lastName;
    private String reason;
    private LocalDate date;
    private String appointmentTime;
    private String status;
    private String profile_picture;
    private String specialization;
}
