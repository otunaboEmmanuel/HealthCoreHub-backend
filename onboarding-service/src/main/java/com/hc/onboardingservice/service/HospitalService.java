package com.hc.onboardingservice.service;

import com.hc.onboardingservice.requests.HospitalRequest;
import com.hc.onboardingservice.requests.UpdateRequest;
import com.hc.onboardingservice.entity.Hospital;
import com.hc.onboardingservice.enums.Status;
import com.hc.onboardingservice.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HospitalService {
    private final HospitalRepository hospitalRepository;
    public Hospital registerHospital(HospitalRequest hospitalRequest) {
        Hospital hospital = hospitalRepository.findByName(hospitalRequest.getName()).orElse(null);
        if (hospital == null)
        {
            Hospital hospital1 = Hospital.builder()
                    .address(hospitalRequest.getAddress())
                    .name(hospitalRequest.getName())
                    .email(hospitalRequest.getEmail())
                    .contactPerson(hospitalRequest.getContactPerson())
                    .status(Status.PENDING)
                    .phoneNumber(hospitalRequest.getPhoneNumber())
                    .createdAt(LocalDateTime.now())
                    .build();
            return hospitalRepository.save(hospital1);
        }
        return null;
    }

    public Hospital updateHospital(Integer id, UpdateRequest updateRequest) {
        Hospital hospital= hospitalRepository.findById(id).orElse(null);
        if (hospital == null)
        {
            return null;
        }
        hospital.setName(updateRequest.getName());
        hospital.setEmail(updateRequest.getEmail());
        hospital.setContactPerson(updateRequest.getContactPerson());
        hospital.setAddress(updateRequest.getAddress());
        hospital.setPhoneNumber(updateRequest.getPhoneNumber());
        hospital.setCreatedAt(LocalDateTime.now());
        hospital.setStatus(updateRequest.getStatus());
        return hospitalRepository.save(hospital);
    }

    public Hospital findHospital(Integer id) {
        return hospitalRepository.findById(id).orElse(null);
    }

    public Hospital deleteHospital(Integer id) {
        Hospital hospital = hospitalRepository.findById(id).orElse(null);
        if (hospital != null)
        {
            hospitalRepository.delete(hospital);
        }
        return hospital;
    }
}
