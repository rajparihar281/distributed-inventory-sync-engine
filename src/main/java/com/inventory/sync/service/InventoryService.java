package com.inventory.sync.service;

import com.inventory.sync.cache.InventoryCacheService;
import com.inventory.sync.domain.*;
import com.inventory.sync.exception.InsufficientStockException;
import com.inventory.sync.repository.InventoryAuditLogRepository;
import com.inventory.sync.repository.InventoryRepository;
import com.inventory.sync.repository.ProductRepository;
import com.inventory.sync.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryAuditLogRepository auditLogRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryCacheService cacheService;

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
        cacheService.evict(productId, warehouseId);

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
        cacheService.evict(productId, warehouseId);
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
        cacheService.evict(productId, warehouseId);
    }

    @Transactional
    public Inventory inwardStock(Long productId, Long warehouseId, int quantity, String poReference) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseIdWithLock(productId, warehouseId)
                .orElseGet(() -> {
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
                    Warehouse warehouse = warehouseRepository.findById(warehouseId)
                            .orElseThrow(() -> new IllegalArgumentException("Warehouse not found with ID: " + warehouseId));
                    return new Inventory(product, warehouse, 0, 0, 0, 0);
                });

        inventory.addPhysicalStock(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        InventoryAuditLog audit = new InventoryAuditLog(
                savedInventory,
                InventoryAuditType.INWARD,
                +quantity,
                +quantity,
                0,
                0,
                poReference,
                "Inward procurement batch: " + poReference
        );
        auditLogRepository.save(audit);
        cacheService.evict(productId, warehouseId);

        return savedInventory;
    }

    @Transactional
    public Inventory markStockDamaged(Long productId, Long warehouseId, int quantity, String reason) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseIdWithLock(productId, warehouseId)
                .orElseThrow(() -> new IllegalStateException("Inventory record not found for damage quarantine"));

        inventory.markAsDamaged(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        InventoryAuditLog audit = new InventoryAuditLog(
                savedInventory,
                InventoryAuditType.DAMAGE_QUARANTINE,
                0,
                -quantity,
                0,
                +quantity,
                "DMG-" + System.currentTimeMillis(),
                reason
        );
        auditLogRepository.save(audit);
        cacheService.evict(productId, warehouseId);

        return savedInventory;
    }

    @Transactional(readOnly = true)
    public Inventory getInventory(Long productId, Long warehouseId) {
        return inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory record not found"));
    }
}