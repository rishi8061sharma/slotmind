package com.slotmind.service;

import com.slotmind.exception.ResourceNotFoundException;
import com.slotmind.exception.BadRequestException;
import com.slotmind.model.Slot;
import com.slotmind.model.SlotStatus;
import com.slotmind.repository.SlotRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlotStatusService {
    private final SlotRepository slotRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SlotStatusService(SlotRepository slotRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.slotRepository = slotRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Slot updateStatus(String slotId, SlotStatus status) {
        Slot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Slot not found: " + slotId));
        if (status != SlotStatus.CANCELLED) {
            throw new BadRequestException("This endpoint currently supports CANCELLED only");
        }
        slot.cancel();
        Slot saved = slotRepository.save(slot);

        if (status == SlotStatus.CANCELLED) {
            eventPublisher.publishEvent(new SlotCancelledEvent(slotId));
        }
        return saved;
    }
}
