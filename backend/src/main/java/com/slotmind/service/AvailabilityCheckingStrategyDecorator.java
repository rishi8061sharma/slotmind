package com.slotmind.service;

import com.slotmind.model.ProviderStatus;
import com.slotmind.model.SlotStatus;

public class AvailabilityCheckingStrategyDecorator implements SchedulingStrategy {
    private final SchedulingStrategy delegate;

    public AvailabilityCheckingStrategyDecorator(SchedulingStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public java.util.List<Recommendation> recommend(SchedulingContext context) {
        SchedulingContext safeContext = new SchedulingContext(
                context.appointment(),
                context.availableSlots().stream()
                        .filter(slot -> slot.getStatus() == SlotStatus.OPEN)
                        .toList(),
                context.providers().stream()
                        .filter(provider -> provider.getStatus() != ProviderStatus.OFFLINE)
                        .toList(),
                context.requiredDurationMinutes());
        return delegate.recommend(safeContext);
    }
}