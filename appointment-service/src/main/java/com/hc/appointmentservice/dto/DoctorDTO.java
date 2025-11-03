package com.hc.appointmentservice.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DoctorDTO {
    private String firstName;
    private String lastName;
    private Integer doctorId;
    private String specialization;
    private List<String> availability;
    private String profile_picture;
    private String license_number;
}
