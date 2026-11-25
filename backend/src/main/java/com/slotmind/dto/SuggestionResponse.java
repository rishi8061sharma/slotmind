package com.slotmind.dto;

import com.slotmind.model.RescheduleSuggestion;
import java.math.BigDecimal;

public record SuggestionResponse(String id, String appointmentId, String recommendedSlotId,
                                 String alternateProviderId, BigDecimal confidence,
                                 String reasoning, String status, String triggerReason) {
    public static SuggestionResponse from(RescheduleSuggestion s) {
        return new SuggestionResponse(s.getId(), s.getAppointmentId(), s.getRecommendedSlotId(),
                s.getAlternateProviderId(), s.getConfidence(), s.getReasoning(),
                s.getStatus().name(), s.getTriggerReason().name());
    }
}