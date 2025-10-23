package com.hc.onboardingservice.dto;

import com.hc.onboardingservice.entity.Hospital;
import com.hc.onboardingservice.entity.HospitalAdmin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalListResponse {
    private Integer id;
    private String name;
    private String state;
    private String city;
    private String address;




    // Constructor to map from Hospital entity
    public HospitalListResponse(Hospital hospital) {
        this.id = hospital.getId();
        this.name = hospital.getName();
        this.state = hospital.getState();
        this.city = hospital.getCity();
        this.address = hospital.getAddress();

    }
}