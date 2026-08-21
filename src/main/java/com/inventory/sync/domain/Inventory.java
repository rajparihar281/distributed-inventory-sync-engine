package com.inventory.sync.domain;

import com.inventory.sync.exception.InsufficientStockException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_product_warehouse", columnNames = {"product_id", "warehouse_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "physical_qty", nullable = false)
    private Integer physicalQty = 0;

    @Column(name = "available_qty", nullable = false)
    private Integer availableQty = 0;

    @Column(name = "reserved_qty", nullable = false)
    private Integer reservedQty = 0;

    @Column(name = "damaged_qty", nullable = false)
    private Integer damagedQty = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Inventory(Product product, Warehouse warehouse, int initialPhysicalQty) {
        if (initialPhysicalQty < 0) {
            throw new IllegalArgumentException("Initial quantity cannot be negative");
        }
        this.product = product;
        this.warehouse = warehouse;
        this.physicalQty = initialPhysicalQty;
        this.availableQty = initialPhysicalQty;
        this.reservedQty = 0;
        this.damagedQty = 0;
        validateInvariant();
    }

    // --- Domain Mutation Methods ---

    public void reserveStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive");
        }
        if (this.availableQty < quantity) {
            throw new InsufficientStockException(
                    "Insufficient available stock for product " + product.getSku() +
                            ". Requested: " + quantity + ", Available: " + this.availableQty
            );
        }
        this.availableQty -= quantity;
        this.reservedQty += quantity;
        validateInvariant();
    }

    public void releaseReservation(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Release quantity must be positive");
        }
        if (this.reservedQty < quantity) {
            throw new IllegalStateException("Cannot release more than currently reserved");
        }
        this.reservedQty -= quantity;
        this.availableQty += quantity;
        validateInvariant();
    }

    public void dispatchReservedStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Dispatch quantity must be positive");
        }
        if (this.reservedQty < quantity || this.physicalQty < quantity) {
            throw new IllegalStateException("Insufficient reserved/physical stock to dispatch");
        }
        this.reservedQty -= quantity;
        this.physicalQty -= quantity;
        validateInvariant();
    }

    public void receiveStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Received stock must be positive");
        }
        this.physicalQty += quantity;
        this.availableQty += quantity;
        validateInvariant();
    }

    private void validateInvariant() {
        if (physicalQty < 0 || availableQty < 0 || reservedQty < 0 || damagedQty < 0) {
            throw new IllegalStateException("Inventory bucket values cannot be negative");
        }
        if (physicalQty != (availableQty + reservedQty + damagedQty)) {
            throw new IllegalStateException(
                    "Inventory invariant broken: Physical (" + physicalQty +
                            ") != Available (" + availableQty + ") + Reserved (" + reservedQty + ") + Damaged (" + damagedQty + ")"
            );
        }
    }
}