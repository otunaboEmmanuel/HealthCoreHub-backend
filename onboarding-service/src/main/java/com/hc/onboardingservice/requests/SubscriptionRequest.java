package com.hc.onboardingservice.requests;

import com.hc.onboardingservice.enums.Plan;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionRequest {
    @NotNull(message = "Plan must not be null")
    @Enumerated(EnumType.STRING)
    private Plan name;

    @NotNull(message = "Price must not be null")
    private Double price;

    @NotNull(message = "Duration must not be null")
    private Integer durationInDays;
}
