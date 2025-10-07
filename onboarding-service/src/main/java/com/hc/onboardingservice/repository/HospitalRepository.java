package com.hc.onboardingservice.repository;

import com.hc.onboardingservice.entity.Hospital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Integer> {

    Optional<Hospital> findByName(String name);

    @Query("""
           SELECT hospital
           FROM Hospital hospital
           """)
    Page<Hospital> findAllHospital(Pageable pageable);
}
