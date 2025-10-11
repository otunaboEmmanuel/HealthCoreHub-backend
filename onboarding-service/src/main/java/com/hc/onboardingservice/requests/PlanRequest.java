package com.hc.onboardingservice.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRequest {
    private Integer id;
    private String name;          // e.g. "Premium Plan"
    private String description;   // e.g. "Access to advanced onboarding features"
    private double price;
    private int durationDays;
    private List<String> features;
}
