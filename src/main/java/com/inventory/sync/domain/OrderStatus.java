package com.inventory.sync.domain;

public enum OrderStatus {
    CREATED,
    RESERVED,
    CONFIRMED,
    PACKING,
    DISPATCHED,
    DELIVERED,
    CANCELLED
}