package com.hc.onboardingservice.requests;
import com.hc.onboardingservice.enums.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRequest {
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
}
