package com.hc.onboardingservice.repository;

import com.hc.onboardingservice.entity.Hospital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Integer> {

    Optional<Hospital> findByName(String name);

    Optional<Hospital> findByDbName(String dbName);

    Optional<Hospital> findByEmail(String hospitalEmail);

    @Query("SELECT DISTINCT h FROM Hospital h " +
            "LEFT JOIN FETCH h.plan " +
            "LEFT JOIN FETCH h.admins " +
            "ORDER BY h.id DESC")
    Page<Hospital> findAllWithDetails(Pageable pageable);

    @EntityGraph(attributePaths = {"plan", "admins"})
    Page<Hospital> findAllHospital(Pageable pageable);

}
