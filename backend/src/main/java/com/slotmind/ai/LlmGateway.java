package com.slotmind.ai;

import com.slotmind.service.SchedulingContext;
import com.slotmind.service.SchedulingStrategy;

public interface LlmGateway {
    SchedulingStrategy.Recommendation recommend(SchedulingContext context);
}