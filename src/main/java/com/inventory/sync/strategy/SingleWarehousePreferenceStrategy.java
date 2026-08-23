package com.inventory.sync.strategy;

import com.inventory.sync.domain.Inventory;
import com.inventory.sync.domain.Product;
import com.inventory.sync.exception.InsufficientStockException;
import com.inventory.sync.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component("singleWarehouseStrategy")
@RequiredArgsConstructor
public class SingleWarehousePreferenceStrategy implements FulfillmentStrategy {

    private final InventoryRepository inventoryRepository;

    @Override
    public List<WarehouseAllocation> allocateStock(Product product, int requiredQuantity) {
        List<Inventory> inventories = inventoryRepository.findByProductId(product.getId());

        // Find the warehouse with enough stock, picking the one with lowest available overhead
        return inventories.stream()
                .filter(inv -> inv.getWarehouse().getIsActive())
                .filter(inv -> inv.getAvailableQty() >= requiredQuantity)
                .min(Comparator.comparingInt(Inventory::getAvailableQty))
                .map(inv -> Collections.singletonList(new WarehouseAllocation(inv.getWarehouse(), requiredQuantity)))
                .orElseThrow(() -> new InsufficientStockException(
                        "No single warehouse has sufficient stock (" + requiredQuantity + " units) for SKU: " + product.getSku()
                ));
    }

    @Override
    public String getStrategyName() {
        return "SINGLE_WAREHOUSE";
    }
}