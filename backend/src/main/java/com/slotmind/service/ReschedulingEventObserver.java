package com.slotmind.service;

import com.slotmind.model.AppointmentStatus;
import com.slotmind.model.TriggerReason;
import com.slotmind.repository.AppointmentRepository;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ReschedulingEventObserver {
    private final AppointmentRepository appointmentRepository;
    private final SuggestionService suggestionService;

    public ReschedulingEventObserver(AppointmentRepository appointmentRepository, SuggestionService suggestionService) {
        this.appointmentRepository=appointmentRepository; this.suggestionService=suggestionService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSlotCancelled(SlotCancelledEvent event) {
        appointmentRepository.findBySlotId(event.slotId())
                .filter(a -> a.getStatus()==AppointmentStatus.CONFIRMED)
                .ifPresent(a -> processSafely(a.getId(), TriggerReason.SLOT_CANCELLED));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProviderOffline(ProviderOfflineEvent event) {
        List<String> appointmentIds=appointmentRepository
                .findByProviderIdAndStatus(event.providerId(), AppointmentStatus.CONFIRMED)
                .stream().map(a -> a.getId()).toList();
        appointmentIds.forEach(id -> processSafely(id, TriggerReason.PROVIDER_OFFLINE));
    }

    private void processSafely(String appointmentId, TriggerReason trigger) {
        try { suggestionService.createTriggered(appointmentId, trigger); }
        catch (RuntimeException ignored) { /* isolate one failed appointment from the batch */ }
    }
}