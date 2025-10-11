package com.hc.onboardingservice.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HospitalRequest {
    @NotBlank(message = "field must not be blank")
    private String name;
    @Email
    @NotBlank(message = "field must not be blank")
    private String email;
    @NotBlank(message = "field must not be blank")
    private String address;
    @NotBlank(message = "field must not be blank")
    private String phoneNumber;
    @NotBlank(message = "field must not be blank")
    private String city;
    @NotBlank(message = "field must not be blank")
    private String state;
    @NotBlank(message = "field must not be blank")
    private String hospitalType;
    @NotBlank(message = "field must not be blank")
    private String country;
    private PlanRequest plan;
}
