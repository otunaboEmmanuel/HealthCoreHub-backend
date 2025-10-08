package com.hc.onboardingservice.controller;

import com.hc.onboardingservice.entity.SubscriptionPlan;
import com.hc.onboardingservice.requests.SubscriptionRequest;
import com.hc.onboardingservice.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {
    private final SubscriptionPlanService subscriptionPlanService;
    @PostMapping()
    public ResponseEntity<?> createSubscriptionPlan(@RequestBody SubscriptionRequest request) {
        log.info("Plan received: {}", request.getName());
        SubscriptionPlan plan = subscriptionPlanService.subscribe(request);
        Map<String,Object> response = new HashMap<>();
        if (plan == null) {
            response.put("code", "100");
            response.put("message", "the " +request.getName() +" plan already exists".toUpperCase());
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
        response.put("code", "00");
        response.put("message","plan created successfully".toUpperCase());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @GetMapping()
    public ResponseEntity<?> getSubscriptionPlans()
    {
        List<SubscriptionPlan> plan = subscriptionPlanService.findAll();
        return new ResponseEntity<>(plan, HttpStatus.OK);
    }
}
