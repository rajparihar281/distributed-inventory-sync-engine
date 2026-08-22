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

    /**
     * Reserves stock using an explicit Pessimistic Lock at the database row level.
     * Prevents race conditions and overselling across concurrent threads.
     */
    @Transactional
    public Inventory reserveStockWithLock(Long productId, Long warehouseId, int quantity, String orderNumber) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseIdWithLock(productId, warehouseId)
                .orElseThrow(() -> new InsufficientStockException(
                        "No inventory record found for product ID " + productId + " at warehouse ID " + warehouseId
                ));

        // 1. Mutate domain state (guards check availability internally)
        inventory.reserveStock(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 2. Append immutable audit ledger
        InventoryAuditLog audit = new InventoryAuditLog(
                savedInventory,
                InventoryAuditType.RESERVATION_HOLD,
                0,              // delta physical
                -quantity,      // delta available
                +quantity,      // delta reserved
                0,              // delta damaged
                orderNumber,
                "Stock reserved for Order #" + orderNumber
        );
        auditLogRepository.save(audit);

        return savedInventory;
    }

    /**
     * Releases previously reserved stock back to available buckets (e.g. on cancellation).
     */
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
}