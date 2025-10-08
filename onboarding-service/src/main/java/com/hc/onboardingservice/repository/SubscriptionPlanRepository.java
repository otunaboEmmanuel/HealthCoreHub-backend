package com.hc.onboardingservice.repository;

import com.hc.onboardingservice.entity.SubscriptionPlan;
import com.hc.onboardingservice.enums.Plan;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Integer> {
    Optional<SubscriptionPlan> findByName(Plan name);
}
