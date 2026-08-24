package com.inventory.sync.controller;

import com.inventory.sync.domain.Inventory;
import com.inventory.sync.dto.InwardStockRequest;
import com.inventory.sync.dto.InventoryResponse;
import com.inventory.sync.dto.MarkDamagedRequest;
import com.inventory.sync.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/inward")
    public ResponseEntity<InventoryResponse> inwardStock(@Valid @RequestBody InwardStockRequest request) {
        Inventory inv = inventoryService.inwardStock(
                request.getProductId(),
                request.getWarehouseId(),
                request.getQuantity(),
                request.getPurchaseOrderRef()
        );
        return ResponseEntity.ok(mapToResponse(inv));
    }

    @PostMapping("/mark-damaged")
    public ResponseEntity<InventoryResponse> markDamaged(@Valid @RequestBody MarkDamagedRequest request) {
        Inventory inv = inventoryService.markStockDamaged(
                request.getProductId(),
                request.getWarehouseId(),
                request.getQuantity(),
                request.getReason()
        );
        return ResponseEntity.ok(mapToResponse(inv));
    }

    @GetMapping
    public ResponseEntity<InventoryResponse> getInventory(
            @RequestParam Long productId,
            @RequestParam Long warehouseId
    ) {
        Inventory inv = inventoryService.getInventory(productId, warehouseId);
        return ResponseEntity.ok(mapToResponse(inv));
    }

    private InventoryResponse mapToResponse(Inventory inv) {
        return InventoryResponse.builder()
                .inventoryId(inv.getId())
                .productId(inv.getProduct().getId())
                .productSku(inv.getProduct().getSku())
                .productName(inv.getProduct().getName())
                .warehouseId(inv.getWarehouse().getId())
                .warehouseCode(inv.getWarehouse().getCode())
                .physicalQty(inv.getPhysicalQty())
                .availableQty(inv.getAvailableQty())
                .reservedQty(inv.getReservedQty())
                .damagedQty(inv.getDamagedQty())
                .version(inv.getVersion())
                .updatedAt(inv.getUpdatedAt())
                .build();
    }
}