package com.inventory.sync.repository;

import com.inventory.sync.domain.InventoryAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryAuditLogRepository extends JpaRepository<InventoryAuditLog, Long> {
    List<InventoryAuditLog> findByInventoryId(Long inventoryId);
    List<InventoryAuditLog> findByReferenceId(String referenceId);
}