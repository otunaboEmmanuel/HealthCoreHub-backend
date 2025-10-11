package com.hc.onboardingservice.dto;

import com.hc.onboardingservice.entity.Hospital;
import com.hc.onboardingservice.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HospitalDto {
    private Integer id;
    private String name;
    private String address;
    private String email;
    private String city;
    private String state;
    private String hospitalType;
    private String phoneNumber;
    private LocalDateTime createdAt;
    //complete the constructor
    public HospitalDto(Hospital hospital) {
        this.id = hospital.getId();
        this.name = hospital.getName();
        this.address= hospital.getAddress();
        this.email = hospital.getEmail();
        this.city = hospital.getCity();
        this.state = hospital.getState();
        this.hospitalType = hospital.getHospitalType();
        this.phoneNumber=hospital.getPhone();
        this.createdAt=hospital.getCreatedAt();
    }
}
