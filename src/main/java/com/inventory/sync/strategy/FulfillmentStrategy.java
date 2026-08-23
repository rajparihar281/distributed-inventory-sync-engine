package com.inventory.sync.strategy;

import com.inventory.sync.domain.Product;

import java.util.List;

public interface FulfillmentStrategy {
    List<WarehouseAllocation> allocateStock(Product product, int requiredQuantity);
    String getStrategyName();
}