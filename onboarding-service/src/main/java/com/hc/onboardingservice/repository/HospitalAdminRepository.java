package com.hc.onboardingservice.repository;

import com.hc.onboardingservice.entity.HospitalAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HospitalAdminRepository extends JpaRepository<HospitalAdmin, Integer> {
    boolean existsByEmail(String adminEmail);
}
