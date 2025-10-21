package com.hc.hospitalservice.dto;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequest {
    @NotBlank(message = "first name can not be null")
    private String firstName;
    @NotBlank(message = "last name can not be null")
    private String lastName;
    private String middleName;
    @Email(message = "email must be written in the right setting")
    private String email;
    private String phoneNumber;
    @NotBlank(message = "password can not be null")
    private String password;
    private Integer hospitalId;
}
