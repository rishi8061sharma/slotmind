package com.slotmind.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SchedulingStrategyRegistry {
    private final Map<String, SchedulingStrategy> strategies;

    public SchedulingStrategyRegistry(Map<String, SchedulingStrategy> strategies) {
        this.strategies = strategies;
    }

    public SchedulingStrategy get(String name) {
        SchedulingStrategy strategy = strategies.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown scheduling strategy: " + name);
        }
        return strategy;
    }
}