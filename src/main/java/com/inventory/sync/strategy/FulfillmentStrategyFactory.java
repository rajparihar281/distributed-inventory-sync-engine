package com.inventory.sync.strategy;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class FulfillmentStrategyFactory {

    private final Map<String, FulfillmentStrategy> strategies;

    public FulfillmentStrategyFactory(Map<String, FulfillmentStrategy> strategies) {
        this.strategies = strategies;
    }

    public FulfillmentStrategy getStrategy(String strategyName) {
        // Fallback to split shipment if not specified
        if (strategyName == null || strategyName.isBlank()) {
            return strategies.get("splitShipmentStrategy");
        }

        return Optional.ofNullable(strategies.get(strategyName))
                .orElse(strategies.get("splitShipmentStrategy"));
    }
}