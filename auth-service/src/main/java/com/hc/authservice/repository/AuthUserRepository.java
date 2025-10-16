package com.hc.authservice.repository;

import com.hc.authservice.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser,String> {
    Optional<AuthUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
