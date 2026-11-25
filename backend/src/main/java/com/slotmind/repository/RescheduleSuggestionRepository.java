package com.slotmind.repository;

import com.slotmind.model.RescheduleSuggestion;
import com.slotmind.model.SuggestionStatus;
import com.slotmind.model.TriggerReason;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface RescheduleSuggestionRepository extends JpaRepository<RescheduleSuggestion, String> {
    List<RescheduleSuggestion> findByStatus(SuggestionStatus status);
    Optional<RescheduleSuggestion> findFirstByAppointmentIdAndTriggerReasonAndStatus(String appointmentId, TriggerReason triggerReason, SuggestionStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RescheduleSuggestion r where r.id = :id")
    Optional<RescheduleSuggestion> findByIdForUpdate(@Param("id") String id);
}