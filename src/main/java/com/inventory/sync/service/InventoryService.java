package com.inventory.sync.service;

import com.inventory.sync.domain.*;
import com.inventory.sync.exception.InsufficientStockException;
import com.inventory.sync.repository.InventoryAuditLogRepository;
import com.inventory.sync.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryAuditLogRepository auditLogRepository;

    @Transactional
    public Inventory reserveStockWithLock(Long productId, Long warehouseId, int quantity, String orderNumber) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseIdWithLock(productId, warehouseId)
                .orElseThrow(() -> new InsufficientStockException(
                        "No inventory record found for product ID " + productId + " at warehouse ID " + warehouseId
                ));

        inventory.reserveStock(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        InventoryAuditLog audit = new InventoryAuditLog(
                savedInventory,
                InventoryAuditType.RESERVATION_HOLD,
                0,
                -quantity,
                +quantity,
                0,
                orderNumber,
                "Stock reserved for Order #" + orderNumber
        );
        auditLogRepository.save(audit);

        return savedInventory;
    }

    @Transactional
    public void releaseReservation(Long productId, Long warehouseId, int quantity, String orderNumber) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseIdWithLock(productId, warehouseId)
                .orElseThrow(() -> new IllegalStateException("Inventory record not found for release"));

        inventory.releaseReservation(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        InventoryAuditLog audit = new InventoryAuditLog(
                savedInventory,
                InventoryAuditType.RESERVATION_RELEASE,
                0,
                +quantity,
                -quantity,
                0,
                orderNumber,
                "Reservation released for Order #" + orderNumber
        );
        auditLogRepository.save(audit);
    }

    @Transactional
    public void dispatchStock(Long productId, Long warehouseId, int quantity, String orderNumber) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseIdWithLock(productId, warehouseId)
                .orElseThrow(() -> new IllegalStateException("Inventory record not found for dispatch"));

        inventory.dispatchReservedStock(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        InventoryAuditLog audit = new InventoryAuditLog(
                savedInventory,
                InventoryAuditType.DISPATCH,
                -quantity,
                0,
                -quantity,
                0,
                orderNumber,
                "Stock physically dispatched for Order #" + orderNumber
        );
        auditLogRepository.save(audit);
    }
}