package com.inventory.sync.domain;

public enum InventoryAuditType {
    PURCHASE_RECEIPT,
    RESERVATION_HOLD,
    RESERVATION_RELEASE,
    DISPATCH,
    DAMAGE_WRITE_OFF,
    MANUAL_ADJUSTMENT
}