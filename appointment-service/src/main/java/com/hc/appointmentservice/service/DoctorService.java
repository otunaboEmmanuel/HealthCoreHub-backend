package com.hc.appointmentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hc.appointmentservice.dto.*;
import com.hc.appointmentservice.entity.Appointment;
import com.hc.appointmentservice.enums.Status;
import com.hc.appointmentservice.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.*;

import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DoctorService {

    @Value("${tenant.datasource.host}")
    private String tenantDbHost;

    @Value("${tenant.datasource.port}")
    private String tenantDbPort;

    @Value("${tenant.datasource.username}")
    private String tenantDbUsername;

    @Value("${tenant.datasource.password}")
    private String tenantDbPassword;

    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> setDoctorAvailability(Integer id, UpdateDoctorRequest request, String tenantDb) {
        String sql = "SELECT 1 FROM doctors WHERE id = ?";
        if (!checkIdExist(id, tenantDb, sql)) {
            throw new IllegalArgumentException("Doctor with id " + id + " does not exist");
        }

        setDoctorAppointment(id, request, tenantDb);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Doctor has been updated successfully");
        return response;
    }

    private void setDoctorAppointment(Integer id, UpdateDoctorRequest request, String tenantDb) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);
        String sql = """
                UPDATE doctors
                SET availability = ?::jsonb
                WHERE id = ?
                """;
        try (Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ObjectMapper mapper = new ObjectMapper();
            String availabilityJson = mapper.writeValueAsString(request.getAvailability());

            stmt.setString(1, availabilityJson);
            stmt.setInt(2, id);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalStateException("No doctor found with id " + id);
            }
            log.info("Updated doctor {} availability. Rows affected: {}", id, rowsAffected);
        } catch (SQLException exception) {
            log.error("Error updating doctor appointment for id: {}", id, exception);
            throw new RuntimeException("Failed to update doctor availability: " + exception.getMessage(), exception);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkIdExist(Integer id, String tenantDb, String sql) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);
        try (Connection connection = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Error checking if doctor exists with id: {}", id, e);
            throw new RuntimeException("Database error while checking doctor existence", e);
        }
    }

    public List<DoctorDTO> getAllDoctors(String tenantDb) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",
                tenantDbHost, tenantDbPort, tenantDb);

        String sql = """
            SELECT 
                u.first_name,
                u.last_name,
                u.profile_picture,
                d.specialization,
                d.availability,
                d.id,
                d.license_number
            FROM doctors d
            INNER JOIN users u ON d.user_id = u.id
            """;

        try (Connection connection = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            List<DoctorDTO> doctorDTOList = new ArrayList<>();

            while (rs.next()) {
                DoctorDTO doctorDTO = DoctorDTO.builder()
                        .firstName(rs.getString("first_name"))
                        .lastName(rs.getString("last_name"))
                        .doctorId(rs.getInt("id"))
                        .profile_picture(rs.getString("profile_picture"))
                        .specialization(rs.getString("specialization"))
                        .license_number(rs.getString("license_number"))
                        .availability(parseAvailability(rs.getString("availability")))  // ← Parse JSON
                        .build();
                doctorDTOList.add(doctorDTO);
            }

            return doctorDTOList;

        } catch (SQLException e) {
            log.error("Error fetching all doctors: {}", e.getMessage(), e);
            throw new RuntimeException("Database error while fetching doctors", e);
        }
    }

    // Helper method to parse JSON availability
    private List<String> parseAvailability(String availabilityJson) {
        if (availabilityJson == null || availabilityJson.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // If using Jackson (common in Spring Boot)
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(availabilityJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse availability JSON: {}", availabilityJson, e);
            return Collections.singletonList(availabilityJson);  // Fallback
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public List<DoctorResponse> getAppointments(Integer doctorId, String tenantDb) {
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);
        if (appointments.isEmpty()) {
            log.info("No doctor found with id {}", doctorId);
            return Collections.emptyList();
        }
       Set<Integer> patientIds = appointments.stream().map(Appointment::getPatientId).collect(Collectors.toSet());
        Map<Integer, PatientInfo> patientInfoMap = getUserInfo(tenantDb,patientIds);
        return appointments.stream()
                .map(appointment -> {
                    PatientInfo patientInfo= patientInfoMap.get(appointment.getPatientId());
                    return DoctorResponse.builder()
                            .firstName(patientInfo != null ? patientInfo.getFirstName() : "Unknown")
                            .lastName(patientInfo != null ? patientInfo.getLastName() : "")
                            .reason(appointment.getReason())
                            .date(appointment.getDate())
                            .appointmentTime(appointment.getAppointmentTime())
                            .build();
                })
                .collect(Collectors.toList());
    }
    private Map<Integer, PatientInfo> getUserInfo(String tenantDb, Set<Integer> patientIds) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s",tenantDbHost, tenantDbPort, tenantDb);
        String placeHolder = String.join(",",Collections.nCopies(patientIds.size(),"?"));
        String sql = String.format("""
            SELECT\s
                p.id,
                u.first_name,
                u.last_name
            FROM patients p
            INNER JOIN users u ON p.user_id = u.id
            WHERE p.id IN (%s)
           \s""", placeHolder);
        try(Connection conn = DriverManager.getConnection(tenantUrl,tenantDbUsername,tenantDbPassword);
                                    PreparedStatement statement = conn.prepareStatement(sql) ){
           int index = 1;
           for (Integer patientId : patientIds) {
               statement.setInt(index++,patientId);
           }
           Map<Integer, PatientInfo> userInfo = new HashMap<>();
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                userInfo.put(
                        rs.getInt("id"),
                        PatientInfo.builder()
                                .firstName(rs.getString("first_name"))
                                .lastName(rs.getString("last_name"))
                                .build()
                );
            }
            return userInfo;
        }catch (SQLException e) {
            log.error("Error fetching users: {}", e.getMessage(), e);
            throw new RuntimeException("Database error while fetching  user", e);
        }

    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateStatus(Map<String, String> request, Integer appointmentId, String tenantDb) {
        log.info("getting patient id {}", appointmentId);
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            log.warn("appointment not found for id {}", appointmentId);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "No appointment found with id " + appointmentId);
            return result;
        }
        if(request.get("status") == null || request.get("status").isEmpty()) {
            log.warn("bad request status");
            throw new IllegalArgumentException("bad request status");
        }
        appointment.setStatus(Status.valueOf(request.get("status").toUpperCase()));
        appointmentRepository.save(appointment);
        if(appointment.getStatus() == Status.CONFIRMED) {
            log.info("sending email to patient {}", appointmentId);
            PatientInfo patientInfo = getPatientDetails(tenantDb,appointment.getPatientId());
            DoctorInfo doctorInfo = getDoctorInfo(tenantDb, appointment.getDoctorId());
            String DocName = doctorInfo.getFirstName() + " " + doctorInfo.getLastName();
            emailService.sendSimpleMail(patientInfo.getEmail(),patientInfo.getFirstName(),appointment.getAppointmentTime(),DocName);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "appointment updated");
        return result;
        //supposed to send email(i forgot) extract email from user table from patient
    }

    private DoctorInfo getDoctorInfo(String tenantDb, Integer doctorId)  {
         String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
          String sql = """
                  SELECT u.last_name,u.first_name
                  FROM doctors d
                  INNER JOIN users u ON d.user_id = u.id
                  WHERE d.id = ?
                  """ ;
          try(Connection conn = DriverManager.getConnection(tenantUrl,tenantDbUsername, tenantDbPassword);
                                PreparedStatement statement = conn.prepareStatement(sql) ){
              statement.setInt(1, doctorId);
              try(ResultSet rs = statement.executeQuery()) {
              if (rs.next()){
                  return DoctorInfo.builder()
                          .firstName(rs.getString("first_name"))
                          .lastName(rs.getString("last_name"))
                          .build();
              }
              throw new IllegalArgumentException("Doctor not found with id: " + doctorId);
              }
          }catch (SQLException e) {
              log.error("Error fetching users: {}", e.getMessage(), e);
              throw new RuntimeException("Database error while fetching  doctor", e);
          }
          
    }

    private PatientInfo getPatientDetails(String tenantDb, Integer patientId) {
        String tenantUrl = String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
        String sql = """
                SELECT u.first_name,u.email
                FROM patients p
                INNER JOIN users u ON p.user_id = u.id
                WHERE p.id = ?
                """;
        try(Connection conn = DriverManager.getConnection(tenantUrl, tenantDbUsername,tenantDbPassword);
                                PreparedStatement statement = conn.prepareStatement(sql) ){
            statement.setInt(1, patientId);
            try(ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return PatientInfo.builder()
                            .firstName(rs.getString("first_name"))
                            .email(rs.getString("email"))
                            .build();
                }
                throw new IllegalArgumentException("Doctor not found with id: " + patientId);
            }
        }catch (SQLException e){
            log.error("Error fetching users: {}", e.getMessage(), e);
            throw new RuntimeException("Database error while fetching  patient", e);
        }
    }

    public DoctorDTO getDoctorByEmail(String email, String tenantDb) {
          String tenantUrl= String.format("jdbc:postgresql://%s:%s/%s", tenantDbHost, tenantDbPort, tenantDb);
             String sql = """
                     SELECT u.first_name,u.email,
                     u.profile_picture,d.specialization,
                     d.id, d.license_number FROM doctors d
                     INNER JOIN users u ON d.user_id = u.id
                     WHERE u.email = ?
                     """;
             try (Connection connection = DriverManager.getConnection(tenantUrl, tenantDbUsername, tenantDbPassword);
                               PreparedStatement statement = connection.prepareStatement(sql)) {
                   statement.setString(1, email);
                   ResultSet rs = statement.executeQuery();
                 if(rs.next()){
                     return DoctorDTO.builder()
                             .lastName(rs.getString("last_name"))
                             .firstName(rs.getString("first_name"))
                             .profile_picture(rs.getString("profile_picture"))
                             .doctorId(rs.getInt("id"))
                             .license_number(rs.getString("license_number"))
                              .availability(parseAvailability(rs.getString("availability")))  // ← Parse JSON
                             .build();
                 }
                 throw new IllegalArgumentException("Doctor not found with id: " + rs.getInt("id"));

             } catch (SQLException e) {
                 log.error("Error fetching users: {}", e.getMessage(), e);
                 throw new RuntimeException("Database error while fetching  doctor", e);
             }
    }
}