package com.inventory.sync.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_reservations")
@Getter
@Setter
@NoArgsConstructor
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Min(1)
    @Column(name = "reserved_qty", nullable = false)
    private Integer reservedQty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public InventoryReservation(OrderItem orderItem, Warehouse warehouse, int reservedQty) {
        this.orderItem = orderItem;
        this.warehouse = warehouse;
        this.reservedQty = reservedQty;
        this.status = ReservationStatus.ACTIVE;
    }
}