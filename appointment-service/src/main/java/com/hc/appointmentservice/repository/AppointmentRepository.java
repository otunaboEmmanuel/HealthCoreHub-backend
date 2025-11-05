package com.hc.appointmentservice.repository;

import com.hc.appointmentservice.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment,Integer> {
    List<Appointment> findByDoctorId(Integer doctorId);

    boolean existsByPatientId(Integer patientId);
    Optional<Appointment> findByPatientId(Integer patientId);
    List<Appointment> findAllByPatientId(Integer patientId);
}
