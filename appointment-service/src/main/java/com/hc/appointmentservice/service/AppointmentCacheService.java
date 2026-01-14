package com.hc.appointmentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AppointmentCacheService {

    @CacheEvict(value = "appointments", key = "#doctorId + ':' + #tenantDb")
    public void evictDoctorAppointments(Integer doctorId, String tenantDb) {
        log.debug("Evicting cache");
    }
}
