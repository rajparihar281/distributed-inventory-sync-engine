package com.inventory.sync.strategy;

import com.inventory.sync.domain.Inventory;
import com.inventory.sync.domain.Product;
import com.inventory.sync.exception.InsufficientStockException;
import com.inventory.sync.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component("splitShipmentStrategy")
@RequiredArgsConstructor
public class SplitShipmentAllocationStrategy implements FulfillmentStrategy {

    private final InventoryRepository inventoryRepository;

    @Override
    public List<WarehouseAllocation> allocateStock(Product product, int requiredQuantity) {
        List<Inventory> inventories = inventoryRepository.findByProductId(product.getId());

        // Sort descending by available quantity for greedy fulfillment
        List<Inventory> sorted = inventories.stream()
                .filter(inv -> inv.getWarehouse().getIsActive() && inv.getAvailableQty() > 0)
                .sorted(Comparator.comparingInt(Inventory::getAvailableQty).reversed())
                .toList();

        List<WarehouseAllocation> allocations = new ArrayList<>();
        int remaining = requiredQuantity;

        for (Inventory inv : sorted) {
            if (remaining <= 0) break;

            int allocateFromThis = Math.min(inv.getAvailableQty(), remaining);
            allocations.add(new WarehouseAllocation(inv.getWarehouse(), allocateFromThis));
            remaining -= allocateFromThis;
        }

        if (remaining > 0) {
            throw new InsufficientStockException(
                    "Network-wide insufficient stock for SKU: " + product.getSku() +
                            ". Missing " + remaining + " of " + requiredQuantity + " units."
            );
        }

        return allocations;
    }

    @Override
    public String getStrategyName() {
        return "SPLIT_SHIPMENT";
    }
}