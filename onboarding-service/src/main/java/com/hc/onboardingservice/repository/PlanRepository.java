package com.hc.onboardingservice.repository;
import com.hc.onboardingservice.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Integer> {
    Optional<Plan> findByName(String name);
}
