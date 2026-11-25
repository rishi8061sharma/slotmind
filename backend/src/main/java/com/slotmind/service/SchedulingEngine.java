package com.slotmind.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SchedulingEngine {
    private final SchedulingStrategyRegistry registry;
    private final String configuredStrategy;

    public SchedulingEngine(SchedulingStrategyRegistry registry,
                            @Value("${slotmind.scheduling.strategy:rule}") String configuredStrategy) {
        this.registry = registry;
        this.configuredStrategy = configuredStrategy;
    }

    public boolean wasAiFallbackUsed() {
        SchedulingStrategy strategy = registry.get(configuredStrategy);
        return strategy instanceof AiSchedulingStrategy ai && ai.wasFallbackUsed();
    }

    public List<SchedulingStrategy.Recommendation> recommend(SchedulingContext context) {
        try {
            return new AvailabilityCheckingStrategyDecorator(
                    registry.get(configuredStrategy)).recommend(context);
        } catch (RuntimeException strategyFailure) {
            if ("rule".equalsIgnoreCase(configuredStrategy)) throw strategyFailure;
            return new AvailabilityCheckingStrategyDecorator(
                    registry.get("rule")).recommend(context);
        }
    }
}