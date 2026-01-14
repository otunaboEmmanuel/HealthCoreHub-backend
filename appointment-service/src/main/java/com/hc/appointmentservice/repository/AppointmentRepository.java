package com.hc.appointmentservice.repository;

import com.hc.appointmentservice.entity.Appointment;
import com.hc.appointmentservice.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment,Integer> {
    List<Appointment> findByDoctorId(Integer doctorId);

    boolean existsByPatientId(Integer patientId);
    Optional<Appointment> findByPatientId(Integer patientId);
    List<Appointment> findAllByPatientId(Integer patientId);
    Optional<Appointment>findByAppointmentTimeAndDoctorId(String appointmentTime, Integer doctorId);
    Optional<Appointment> findByDate(LocalDate date);

    Optional<Appointment> findByStatus(Status status);
}
