package com.slotmind.service;

import com.slotmind.exception.ConflictException;
import com.slotmind.exception.ResourceNotFoundException;
import com.slotmind.model.*;
import com.slotmind.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class SuggestionService {
    private final RescheduleSuggestionRepository suggestionRepository;
    private final AppointmentRepository appointmentRepository;
    private final SlotRepository slotRepository;
    private final ProviderRepository providerRepository;
    private final SchedulingEngine schedulingEngine;

    public SuggestionService(RescheduleSuggestionRepository suggestionRepository, AppointmentRepository appointmentRepository,
                             SlotRepository slotRepository, ProviderRepository providerRepository, SchedulingEngine schedulingEngine) {
        this.suggestionRepository=suggestionRepository; this.appointmentRepository=appointmentRepository;
        this.slotRepository=slotRepository; this.providerRepository=providerRepository; this.schedulingEngine=schedulingEngine;
    }

    @Transactional
    public RescheduleSuggestion createSuggestion(String appointmentId) {
        return createTriggered(appointmentId, TriggerReason.INITIAL);
    }

    @Transactional
    public RescheduleSuggestion createTriggered(String appointmentId, TriggerReason trigger) {
        var existing=suggestionRepository.findFirstByAppointmentIdAndTriggerReasonAndStatus(appointmentId, trigger, SuggestionStatus.PENDING);
        if (existing.isPresent()) return existing.get();
        Appointment appointment=appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        if (appointment.getStatus()==AppointmentStatus.CONFIRMED) appointment.markReschedulePending();
        List<Provider> providers=providerRepository.findAll();
        List<Slot> openSlots=slotRepository.findByStatusOrderByStartTime(SlotStatus.OPEN);
        var recommendations=schedulingEngine.recommend(new SchedulingContext(appointment, openSlots, providers, 30, trigger));
        if (recommendations.isEmpty()) throw new ConflictException("No eligible reschedule slot found");
        var r=recommendations.get(0);
        RescheduleSuggestion suggestion=new RescheduleSuggestion("RSG-"+UUID.randomUUID(), appointmentId, r.slotId(), r.alternateProviderId(), r.confidence(), r.reasoning(), trigger);
        appointmentRepository.save(appointment);
        return suggestionRepository.save(suggestion);
    }

    @Transactional
    public RescheduleSuggestion decide(String suggestionId, SuggestionStatus decision) {
        RescheduleSuggestion suggestion=suggestionRepository.findByIdForUpdate(suggestionId).orElseThrow(() -> new ResourceNotFoundException("Suggestion not found: " + suggestionId));
        if (suggestion.getStatus()!=SuggestionStatus.PENDING) throw new ConflictException("Suggestion is already finalized");
        if (decision==SuggestionStatus.REJECTED) { suggestion.reject(); return suggestionRepository.save(suggestion); }
        if (decision!=SuggestionStatus.ACCEPTED) throw new ConflictException("Only ACCEPTED or REJECTED is allowed");
        Appointment appointment=appointmentRepository.findByIdForUpdate(suggestion.getAppointmentId()).orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        Slot target=slotRepository.findByIdForUpdate(suggestion.getRecommendedSlotId()).orElseThrow(() -> new ResourceNotFoundException("Recommended slot not found"));
        if (target.getStatus()!=SlotStatus.OPEN) throw new ConflictException("Recommended slot is no longer open");
        Slot old=slotRepository.findByIdForUpdate(appointment.getSlotId()).orElse(null);
        target.book(); if (old!=null && !old.getId().equals(target.getId())) old.open();
        appointment.rescheduleTo(target.getProviderId(), target.getId()); suggestion.accept();
        if (old!=null) slotRepository.save(old); slotRepository.save(target); appointmentRepository.save(appointment);
        return suggestionRepository.save(suggestion);
    }
}