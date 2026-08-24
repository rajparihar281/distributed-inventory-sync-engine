package com.inventory.sync.domain;

public enum InventoryAuditType {
    INWARD,
    RESERVATION_HOLD,
    RESERVATION_RELEASE,
    DISPATCH,
    DAMAGE_QUARANTINE,
    ADJUSTMENT
}