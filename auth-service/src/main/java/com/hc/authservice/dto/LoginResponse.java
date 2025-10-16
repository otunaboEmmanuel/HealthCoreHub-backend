package com.hc.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String refreshToken;
    private String userId;
    private String email;
    private Integer hospitalId;
    private String tenantDb;
    private String globalRole;
    private String tenantRole;
}