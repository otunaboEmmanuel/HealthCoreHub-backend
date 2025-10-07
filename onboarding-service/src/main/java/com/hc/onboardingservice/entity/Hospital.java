package com.hc.onboardingservice.entity;

import com.hc.onboardingservice.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "Hospital")
public class Hospital {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    private String name;
    @NotNull
    private String address;
    @NotNull
    private String email;
    @NotNull
    private String phoneNumber;
    @NotNull
    private String contactPerson;
    @NotNull
    @Enumerated(EnumType.STRING)
    private Status status;
    @NotNull
    private LocalDateTime createdAt;
}
