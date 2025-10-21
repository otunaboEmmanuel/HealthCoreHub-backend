package com.hc.hospitalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalListDto {
    private Integer id;
    private String name;
    private String email;
    private String phone;
    private String type;
    private String city;
    private String state;
    private String country;
    private String fullAddress;
}
