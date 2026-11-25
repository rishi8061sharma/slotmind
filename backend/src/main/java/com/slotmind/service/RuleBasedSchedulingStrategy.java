package com.slotmind.service;

import com.slotmind.model.Appointment;
import com.slotmind.model.Provider;
import com.slotmind.model.ProviderStatus;
import com.slotmind.model.Slot;
import com.slotmind.model.SlotStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component("rule")
public class RuleBasedSchedulingStrategy implements SchedulingStrategy {
    @Override
    public List<Recommendation> recommend(SchedulingContext context) {
        Appointment appointment = context.appointment();
        Map<String, Provider> providers = context.providers().stream()
                .collect(Collectors.toMap(Provider::getId, Function.identity()));
        Provider originalProvider = providers.get(appointment.getProviderId());

        return context.availableSlots().stream()
                .filter(slot -> slot.getStatus() == SlotStatus.OPEN)
                .filter(slot -> slot.getDurationMinutes() != null
                        && slot.getDurationMinutes() >= context.requiredDurationMinutes())
                .filter(slot -> {
                    Provider provider = providers.get(slot.getProviderId());
                    return provider != null && provider.getStatus() != ProviderStatus.OFFLINE;
                })
                .sorted(Comparator
                        .comparing((Slot slot) -> priority(
                                slot, appointment, providers, originalProvider))
                        .thenComparing(Slot::getStartTime))
                .map(slot -> {
                    boolean sameProvider = slot.getProviderId()
                            .equals(appointment.getProviderId());
                    return new Recommendation(
                            slot.getId(),
                            sameProvider ? null : slot.getProviderId(),
                            sameProvider ? BigDecimal.valueOf(0.95) : BigDecimal.valueOf(0.80),
                            sameProvider
                                    ? "Nearest suitable slot with the same provider."
                                    : "Nearest suitable slot with a matching specialty."
                    );
                })
                .toList();
    }

    private int priority(Slot slot, Appointment appointment,
                         Map<String, Provider> providers,
                         Provider originalProvider) {
        if (slot.getProviderId().equals(appointment.getProviderId())) {
            return 0;
        }
        Provider candidate = providers.get(slot.getProviderId());
        if (originalProvider != null && candidate.getSpecialty() != null
                && candidate.getSpecialty() == originalProvider.getSpecialty()) {
            return 1;
        }
        return 2;
    }
}