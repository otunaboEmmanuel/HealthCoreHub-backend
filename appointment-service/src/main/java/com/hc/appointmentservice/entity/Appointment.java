package com.hc.appointmentservice.entity;

import com.hc.appointmentservice.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer userId;
    private Integer doctorId;
    private String reason;
    private LocalDate date;
    private String appointmentTime;
    @Enumerated(EnumType.STRING)
    private Status status;
}
