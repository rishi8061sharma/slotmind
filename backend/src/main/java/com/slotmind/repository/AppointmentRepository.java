package com.slotmind.repository;

import com.slotmind.model.Appointment;
import com.slotmind.model.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {
    List<Appointment> findByStatus(AppointmentStatus status);
    List<Appointment> findByProviderIdAndStatus(String providerId, AppointmentStatus status);
    Optional<Appointment> findBySlotId(String slotId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") String id);
}