package com.hc.hospitalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRequest {
    private String firstName;

    private String middleName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private String role;

    private String status;
}
