package com.hc.onboardingservice.controller;

import com.hc.onboardingservice.dto.HospitalDto;
import com.hc.onboardingservice.dto.HospitalRequest;
import com.hc.onboardingservice.dto.UpdateRequest;
import com.hc.onboardingservice.entity.Hospital;
import com.hc.onboardingservice.repository.HospitalRepository;
import com.hc.onboardingservice.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/hospital")
@RequiredArgsConstructor
public class HospitalController {
    private final HospitalService hospitalService;
    private final HospitalRepository hospitalRepository;
    @PostMapping()
    public ResponseEntity<?> registerHospital(@RequestBody HospitalRequest hospitalRequest) {
        Map<String, String> response = new HashMap<>();
        Hospital hospital = hospitalService.registerHospital(hospitalRequest);
        if (hospital == null) {
            response.put("code", "100");
            response.put("message", "hospital already exists".toUpperCase());
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        else {
            response.put("code", "00");
            response.put("message", "hospital successfully registered".toUpperCase());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
    }
    @GetMapping()
    public Page<?> getRegisteredHospitals(@RequestParam(name = "page",defaultValue = "0", required = false)int page,
                                          @RequestParam(name= "size", defaultValue = "10", required = false) int size)
    {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Hospital> allHospital= hospitalRepository.findAllHospital(pageable);
        return allHospital.map(HospitalDto::new);
    }
    @GetMapping("{id}")
    public ResponseEntity<?> findHospital(@PathVariable Integer id)
    {
        Map<String, String> response = new HashMap<>();
        Hospital hospital = hospitalService.findHospital(id);
        if (hospital == null)
        {
            response.put("code", "101");
            response.put("message", "hospital does not exist");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        return new ResponseEntity<>(hospital, HttpStatus.OK);
    }
    @PutMapping("{id}")
    public ResponseEntity<?>updateHospital(@PathVariable Integer id, @RequestBody UpdateRequest updateRequest)
        {
            Map<String, String> response = new HashMap<>();
            Hospital hospital = hospitalService.updateHospital(id, updateRequest);
            if (hospital == null)
            {
                response.put("code", "100");
                response.put("message", "hospital already exists".toUpperCase());
            }
                response.put("code", "00");
                response.put("message", "hospital successfully updated".toUpperCase());
             return new ResponseEntity<>(response, HttpStatus.OK);
        }

    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteHospital(@PathVariable Integer id)
    {
        Map<String, String> response = new HashMap<>();
        Hospital hospital = hospitalService.deleteHospital(id);
        if (hospital == null)
        {
            response.put("code", "101");
            response.put("message", "hospital does not exist".toUpperCase());
        }
        response.put("code", "00");
        response.put("message", "hospital successfully deleted".toUpperCase());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
