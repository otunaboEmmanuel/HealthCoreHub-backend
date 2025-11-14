package com.hc.authservice.service;

import com.hc.authservice.entity.AuthUser;
import com.hc.authservice.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserActivationService {
    private final AuthUserRepository authUserRepository;
    public Map<String, Object> validateToken(String token) {
        Map<String, Object> response = new HashMap<>();
       AuthUser authUser = authUserRepository.findByActivationToken(token).orElse(null);
       if (authUser == null) {
           log.warn("Invalid activation Token {}", token);
           response.put("error", "Token does not exist");
           return response;
       }
       if(authUser.getTokenExpired().isBefore(LocalDateTime.now())) {
           log.warn("Token has expired");
           response.put("error", "Token has expired");
           return response;
       }
       if(!(authUser.getStatus().equals("PENDING"))){
           log.info("user is already created status is not pending. Status: {}", authUser.getStatus());
           response.put("error", "User is already created status is not pending");
           return response;
        }
       response.put("success", true);
       response.put("message", "Activation Token has been validated successfully");
       return response;
    }
}
