package com.hc.onboardingservice.controller;

import com.hc.onboardingservice.entity.Plan;
import com.hc.onboardingservice.requests.PlanRequest;
import com.hc.onboardingservice.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
@CrossOrigin
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping
    public ResponseEntity<?> createPlan(@RequestBody PlanRequest request) {
        Plan plan = planService.createPlan(request);
        Map<String, String> response = new HashMap<>();
        if(plan == null)
        {
            response.put("code","101");
            response.put("message","Plan already exists".toUpperCase());
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
            response.put("code","00");
            response.put("message","Plan created successfully".toUpperCase());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Plan>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllPlans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Plan> getPlanById(@PathVariable Integer id) {
        return ResponseEntity.ok(planService.getPlanById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Plan> updatePlan(
            @PathVariable Integer id,
            @RequestBody PlanRequest request) {
        return ResponseEntity.ok(planService.updatePlan(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Integer id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}
