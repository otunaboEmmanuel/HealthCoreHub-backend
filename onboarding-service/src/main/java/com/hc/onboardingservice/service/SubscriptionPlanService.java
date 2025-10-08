package com.hc.onboardingservice.service;

import com.hc.onboardingservice.entity.SubscriptionPlan;
import com.hc.onboardingservice.enums.Plan;
import com.hc.onboardingservice.repository.SubscriptionPlanRepository;
import com.hc.onboardingservice.requests.SubscriptionRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class SubscriptionPlanService {
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    public  SubscriptionPlan subscribe(SubscriptionRequest request) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByName(request.getName())
                .orElse(null);

        if (plan == null) {
            SubscriptionPlan subscriptionPlan = SubscriptionPlan.builder()
                    .durationInDays(request.getDurationInDays())
                    .name(request.getName()) // this is already an enum
                    .price(request.getPrice())
                    .build();

            return subscriptionPlanRepository.save(subscriptionPlan);
        }
        return null;
    }

    public List<SubscriptionPlan> findAll() {
        return subscriptionPlanRepository.findAll();
    }
}
