package com.inventory.sync.strategy;

import com.inventory.sync.domain.Warehouse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WarehouseAllocation {
    private Warehouse warehouse;
    private int allocatedQuantity;
}