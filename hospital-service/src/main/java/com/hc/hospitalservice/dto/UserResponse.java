package com.hc.hospitalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private Boolean success;
    private String message;
    private Integer userId;
    private Integer staffId;
    private String authUserId;
    private String email;
    private String role;
}
