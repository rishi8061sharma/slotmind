package com.slotmind.service;

import java.math.BigDecimal;
import java.util.List;

public interface SchedulingStrategy {
    List<Recommendation> recommend(SchedulingContext context);

    record Recommendation(
            String slotId,
            String alternateProviderId,
            BigDecimal confidence,
            String reasoning
    ) { }
}