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
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(name = "hospital_type")
    private String hospitalType;

    private String country;
    private String state;
    private String city;

    @Column(columnDefinition = "TEXT")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column(name = "db_name", nullable = false)
    private String dbName;

    @Column(name = "db_user", nullable = false)
    private String dbUser;

    @Column(name = "db_password", nullable = false)
    private String dbPassword;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
