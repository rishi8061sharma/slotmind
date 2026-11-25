package com.slotmind.service;

import com.slotmind.ai.LlmGateway;
import com.slotmind.model.ProviderStatus;
import com.slotmind.model.Slot;
import com.slotmind.model.SlotStatus;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component("ai")
public class AiSchedulingStrategy implements SchedulingStrategy {
    private final LlmGateway gateway;
    private final RuleBasedSchedulingStrategy fallback;
    private final ThreadLocal<Boolean> fallbackUsed = ThreadLocal.withInitial(() -> false);

    public AiSchedulingStrategy(LlmGateway gateway, RuleBasedSchedulingStrategy fallback) {
        this.gateway = gateway;
        this.fallback = fallback;
    }

    @Override
    public List<Recommendation> recommend(SchedulingContext context) {
        fallbackUsed.set(false);
        try {
            Recommendation recommendation = gateway.recommend(context);
            validate(recommendation, context);
            return List.of(normalize(recommendation));
        } catch (RuntimeException failure) {
            fallbackUsed.set(true);
            return fallback.recommend(context);
        }
    }

    public boolean wasFallbackUsed() {
        return fallbackUsed.get();
    }

    private void validate(Recommendation recommendation, SchedulingContext context) {
        if (recommendation == null || recommendation.confidence() == null
                || recommendation.confidence().compareTo(BigDecimal.ZERO) < 0
                || recommendation.confidence().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException("Invalid AI confidence");
        }
        Slot slot = context.availableSlots().stream()
                .filter(candidate -> candidate.getId().equals(recommendation.slotId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("AI returned unknown slot"));
        if (slot.getStatus() != SlotStatus.OPEN || slot.getDurationMinutes() == null
                || slot.getDurationMinutes() < context.requiredDurationMinutes()) {
            throw new IllegalStateException("AI returned unavailable slot");
        }
        if (recommendation.alternateProviderId() != null && context.providers().stream()
                .noneMatch(provider -> provider.getId().equals(recommendation.alternateProviderId())
                        && provider.getStatus() != ProviderStatus.OFFLINE)) {
            throw new IllegalStateException("AI returned unknown provider");
        }
    }

    private Recommendation normalize(Recommendation recommendation) {
        String text = recommendation.reasoning() == null
                ? "A suitable available slot was recommended."
                : recommendation.reasoning();
        if (text.length() > 500 || text.matches(".*(?i)(stack trace|jdbc|sql exception|embedding|token limit).*")) {
            text = "A suitable available slot was recommended based on availability and provider fit.";
        }
        return new Recommendation(recommendation.slotId(), recommendation.alternateProviderId(),
                recommendation.confidence(), text);
    }
}