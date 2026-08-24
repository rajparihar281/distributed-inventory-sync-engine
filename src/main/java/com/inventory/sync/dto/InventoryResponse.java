package com.inventory.sync.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InventoryResponse {
    private Long inventoryId;
    private Long productId;
    private String productSku;
    private String productName;
    private Long warehouseId;
    private String warehouseCode;
    private Integer physicalQty;
    private Integer availableQty;
    private Integer reservedQty;
    private Integer damagedQty;
    private Long version;
    private LocalDateTime updatedAt;
}