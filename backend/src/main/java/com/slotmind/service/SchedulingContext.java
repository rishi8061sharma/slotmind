package com.slotmind.service;

import com.slotmind.model.Appointment;
import com.slotmind.model.Provider;
import com.slotmind.model.Slot;
import com.slotmind.model.TriggerReason;
import java.util.List;

public record SchedulingContext(Appointment appointment, List<Slot> availableSlots,
                                List<Provider> providers, int requiredDurationMinutes,
                                TriggerReason triggerReason) {
    public SchedulingContext(Appointment appointment, List<Slot> slots, List<Provider> providers) {
        this(appointment, slots, providers, 30, TriggerReason.INITIAL);
    }
    public SchedulingContext(Appointment appointment, List<Slot> slots, List<Provider> providers,
                             int durationMinutes) {
        this(appointment, slots, providers, durationMinutes, TriggerReason.INITIAL);
    }
}