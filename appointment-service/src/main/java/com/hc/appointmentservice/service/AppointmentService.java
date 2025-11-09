package com.hc.appointmentservice.service;

import com.hc.appointmentservice.dto.AppointmentDTO;
import com.hc.appointmentservice.dto.DoctorInfo;
import com.hc.appointmentservice.dto.DoctorResponse;
import com.hc.appointmentservice.dto.PatientDto;
import com.hc.appointmentservice.entity.Appointment;
import com.hc.appointmentservice.enums.Status;
import com.hc.appointmentservice.repository.AppointmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentService {
    @Value("${tenant.datasource.host}")
    private String tenantDbHost;

    @Value("${tenant.datasource.port}")
    private String tenantDbPort;

    @Value("${tenant.datasource.username}")
    private String tenantDbUsername;

    @Value("${tenant.datasource.password}")
    private String tenantDbPassword;
    private final AppointmentRepository appointmentRepository;

    @Transactional(rollbackFor = Exception.class)
    public Appointment bookAppointment(AppointmentDTO appointment, String tenantDb, Integer patientId, Integer doctorId) {
        if(!userExistsInTenant(tenantDb,patientId)){
            log.error("could not find not find id {} in patients table",patientId);
            throw new RuntimeException("patient with id " + patientId + " not exists");
        }
        if(!doctorExists(tenantDb,doctorId)){
            log.error("doctor with id {} not exists",doctorId);
            throw new RuntimeException("doctor with id " + doctorId + " not exists");
        }
        if(appointmentRepository.findByAppointmentTime(appointment.getAppointmentTime()).isPresent()){
            log.warn("appointment time already exists");
            throw new RuntimeException("appointment time already exists");
        }
        Appointment appointment1 = Appointment.builder()
                .appointmentTime(appointment.getAppointmentTime())
                .date(appointment.getDate())
                .doctorId(doctorId)
                .patientId(patientId)
                .reason(appointment.getReason())
                .status(Status.PENDING)
                .build();
         return appointmentRepository.save(appointment1);
    }

    private boolean userExistsInTenant(String tenantDb, Integer patientId) {
        String tenantUrl=String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
        String sql = "SELECT COUNT (*) FROM patients WHERE id = ?";//and status = active
        try(Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername,tenantDbPassword);
                                PreparedStatement statement = conn.prepareStatement(sql) ){
            statement.setInt(1,patientId);
            ResultSet rs = statement.executeQuery();
            return rs.next() && rs.getInt(1) >0;
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
    private boolean doctorExists(String tenantDb, Integer doctorId) {
        String tenantUrl=String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
        String sql = "SELECT COUNT(*) FROM doctors WHERE id = ?";
        try(Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername,tenantDbPassword);
            PreparedStatement statement = conn.prepareStatement(sql) ){
            statement.setInt(1,doctorId);
            ResultSet rs = statement.executeQuery();
            return rs.next() && rs.getInt(1) >0;
        } catch (SQLException e) {
            log.error(" Error checking user existence in tenant {} ", tenantDb);
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> updateAppointment(Map<String, String> request, Integer patientId) {
        Map<String, Object> response = new HashMap<>();
        if(!appointmentRepository.existsByPatientId(patientId)){
            log.error("could not find id {} in appointment table",patientId);
            throw new RuntimeException("appointment with id " + patientId + " not exists");
        }
        Appointment appointment = appointmentRepository.findByPatientId(patientId).orElse(null);
        if(appointment != null) {
            appointment.setAppointmentTime(request.get("appointmentTime"));
            appointment.setDate(LocalDate.parse(request.get("date")));
            appointmentRepository.save(appointment);
            response.put("status", "00");
            response.put("message", "appointment updated successfully");
            return response;
        }
        response.put("status", "error");
        response.put("message", "appointment not found");
        return response;
    }


    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getPatientByEmail(String email, String tenantDb) {
        log.info("get patient by email {}", email);
        PatientDto patientDto = getTenantPatientByEmail(email, tenantDb);
        Map<String, Object> response = new HashMap<>();
        if(patientDto == null){
            response.put("status", "error");
        }
        response.put("patient", patientDto);
        return response;
    }

    private PatientDto getTenantPatientByEmail(String email, String tenantDb) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
        String sql = """
                SELECT
                p.id,
                u.first_name,
                u.middle_name,
                u.last_name,
                u.email,
                u.phone_number,
                u.role,
                p.user_id,
                u.created_at,
                u.status,
                p.hospital_number
                FROM patients p
                INNER JOIN users u ON p.user_id = u.id
                WHERE u.email = ?
                """;
        try(Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
                                PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1,email);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                return PatientDto.builder()
                        .patientId(rs.getInt("id"))
                        .userId(rs.getInt("user_id"))
                        .firstName(rs.getString("first_name"))
                        .middleName(rs.getString("middle_name"))
                        .lastName(rs.getString("last_name"))
                        .email(rs.getString("email"))
                        .createdAt(rs.getObject("created_at", LocalDateTime.class))
                        .phoneNumber(rs.getString("phone_number"))
                        .role(rs.getString("role"))
                        .status(rs.getString("status"))
                        .hospitalNumber(rs.getString("hospital_number"))
                        .build();
            }
            throw new RuntimeException("could not find patient by email " + email);
        }catch (SQLException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    public List<DoctorResponse> getAppointmentByPatient(Integer patientId, String tenantDb) {
        List<Appointment> appointments = appointmentRepository.findAllByPatientId(patientId);
        Set<Integer> doctorIds = appointments.stream().map(Appointment::getDoctorId).collect(Collectors.toSet());
        Map<Integer, DoctorInfo> doctorInfoMap = getDoctorInfo(tenantDb, doctorIds);
        return appointments.stream().map(appointment -> {
            DoctorInfo doctorInfo = doctorInfoMap.get(appointment.getDoctorId());
            if(doctorInfo == null){
                log.error("could not find doctor info for appointment {}", appointment.getId());
                throw new RuntimeException("doctor info for appointment " + appointment.getId() + " not found");
            }
            return DoctorResponse.builder()
                    .appointmentTime(appointment.getAppointmentTime())
                    .lastName(doctorInfo.getLastName())
                    .firstName(doctorInfo.getFirstName())
                    .date(appointment.getDate())
                    .status(String.valueOf(appointment.getStatus()))
                    .profile_picture(doctorInfo.getProfile_picture())
                    .specialization(doctorInfo.getSpecialization())
                    .reason(appointment.getReason())
                    .build();
        }).collect(Collectors.toList());
}

    private Map<Integer, DoctorInfo> getDoctorInfo(String tenantDb, Set<Integer> doctorIds) {
        if(doctorIds == null || doctorIds.isEmpty()){
            log.error("doctorIds is empty");
            throw new RuntimeException("doctorIds is empty");
        }
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
        String placeHolder = String.join(",", Collections.nCopies(doctorIds.size(), "?"));
        String sql = String.format("""
                SELECT u.first_name,
                u.last_name,
                u.profile_picture,
                d.specialization,
                d.id FROM doctors d\s
                INNER JOIN users u ON d.user_id = u.id
                WHERE d.id IN (%s)
               \s""",placeHolder);
        try(Connection conn = DriverManager.getConnection(tenantUrl,tenantDbUsername, tenantDbPassword);
                                PreparedStatement statement = conn.prepareStatement(sql)){
           int  i = 1;
           for(Integer doctorId : doctorIds){
               statement.setInt(i++,doctorId);
           }
           Map<Integer, DoctorInfo> doctorInfoMap = new HashMap<>();
           ResultSet rs = statement.executeQuery();
           while(rs.next()){
               doctorInfoMap.put(
                       rs.getInt("id"),
                       DoctorInfo.builder()
                               .lastName(rs.getString("last_name"))
                               .firstName(rs.getString("first_name"))
                               .profile_picture(rs.getString("profile_picture"))
                               .specialization(rs.getString("specialization"))
                               .build()
               );
           }
           return doctorInfoMap;
        }catch (SQLException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Database error while fetching  doctors", e);
        }
    }
    }
