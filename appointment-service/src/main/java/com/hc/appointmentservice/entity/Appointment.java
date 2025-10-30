package com.hc.appointmentservice.entity;

import com.hc.appointmentservice.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "appointment")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer patientId;
    private Integer doctorId;
    private String reason;
    private LocalDate date;
    private String appointmentTime;
    @Enumerated(EnumType.STRING)
    private Status status;
}
