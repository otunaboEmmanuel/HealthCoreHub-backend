package com.hc.onboardingservice.service;

import com.hc.onboardingservice.entity.Plan;
import com.hc.onboardingservice.repository.PlanRepository;
import com.hc.onboardingservice.requests.PlanRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PlanService {

    private final PlanRepository planRepository;

    public Plan createPlan(PlanRequest request) {
        Plan plan = planRepository.findByName(request.getName()).orElse(null);
        if (plan == null) {
            Plan plan1 = Plan.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .durationDays(request.getDurationDays())
                    .features(request.getFeatures())
                    .createdAt(LocalDateTime.now())
                    .build();
            return planRepository.save(plan1);
        }
        return null;
    }

    public List<Plan> getAllPlans() {
        return planRepository.findAll();
    }

    public Plan getPlanById(Integer id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found with ID: " + id));
    }

    public Plan updatePlan(Integer id, PlanRequest request) {
        Plan plan = getPlanById(id);
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setPrice(request.getPrice());
        plan.setDurationDays(request.getDurationDays());
        plan.setFeatures(request.getFeatures());
        return planRepository.save(plan);
    }

    public void deletePlan(Integer id) {
        planRepository.deleteById(id);
    }
}
