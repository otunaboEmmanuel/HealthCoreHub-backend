package com.hc.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "hospital_id")
    private Integer hospitalId;

    @Column(name = "tenant_db")
    private String tenantDb;

    @Column(name = "global_role", nullable = false)
    private String globalRole;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "email_verified")
    private Boolean emailVerified ;

    @Column(name = "is_locked")
    private Boolean isLocked;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "status")
    private String status;

    @Column(name = "activation_token")
    private String activationToken;

    @Column(name = "token_expired")
    private LocalDateTime tokenExpired;
}