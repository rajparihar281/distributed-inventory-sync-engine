package com.inventory.sync.domain;

import com.inventory.sync.exception.InsufficientStockException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_warehouse", columnNames = {"product_id", "warehouse_id"})
        }
)
@Getter
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
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Inventory(Product product, Warehouse warehouse, Integer physicalQty, Integer availableQty, Integer reservedQty, Integer damagedQty) {
        this.product = product;
        this.warehouse = warehouse;
        this.physicalQty = physicalQty;
        this.availableQty = availableQty;
        this.reservedQty = reservedQty;
        this.damagedQty = damagedQty;
        validateInvariants();
    }

    public void reserveStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive");
        }
        if (this.availableQty < quantity) {
            throw new InsufficientStockException("Insufficient available stock. Requested: " + quantity + ", Available: " + this.availableQty);
        }
        this.availableQty -= quantity;
        this.reservedQty += quantity;
        validateInvariants();
    }

    public void releaseReservation(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Release quantity must be positive");
        }
        if (this.reservedQty < quantity) {
            throw new IllegalStateException("Cannot release more stock than currently reserved");
        }
        this.reservedQty -= quantity;
        this.availableQty += quantity;
        validateInvariants();
    }

    public void dispatchReservedStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Dispatch quantity must be positive");
        }
        if (this.reservedQty < quantity) {
            throw new IllegalStateException("Cannot dispatch more stock than currently reserved");
        }
        this.reservedQty -= quantity;
        this.physicalQty -= quantity;
        validateInvariants();
    }

    public void addPhysicalStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Inward quantity must be positive");
        }
        this.physicalQty += quantity;
        this.availableQty += quantity;
        validateInvariants();
    }

    public void markAsDamaged(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Damaged quantity must be positive");
        }
        if (this.availableQty < quantity) {
            throw new InsufficientStockException("Cannot mark damaged: requested " + quantity + " exceeds available " + this.availableQty);
        }
        this.availableQty -= quantity;
        this.damagedQty += quantity;
        validateInvariants();
    }

    private void validateInvariants() {
        if (this.physicalQty < 0 || this.availableQty < 0 || this.reservedQty < 0 || this.damagedQty < 0) {
            throw new IllegalStateException("Inventory counts cannot be negative");
        }
        if (this.physicalQty != (this.availableQty + this.reservedQty + this.damagedQty)) {
            throw new IllegalStateException(
                    String.format("Invariant violation: physical (%d) != available (%d) + reserved (%d) + damaged (%d)",
                            this.physicalQty, this.availableQty, this.reservedQty, this.damagedQty)
            );
        }
    }
}