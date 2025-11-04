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
public class AppointmentDTO {
//    private Integer patientId;
//    private Integer doctorId;
    private String reason;
    private LocalDate date;
    private String appointmentTime;
}
