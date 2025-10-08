package com.hc.onboardingservice.entity;

import com.hc.onboardingservice.enums.Plan;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "subscription")
public class SubscriptionPlan
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    @Enumerated(EnumType.STRING)
    private Plan name;
    @NotNull
    private Double price;
    @NotNull
    private Integer durationInDays;
}
