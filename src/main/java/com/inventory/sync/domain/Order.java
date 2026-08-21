package com.inventory.sync.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 64)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Order(String orderNumber, OrderChannel channel) {
        this.orderNumber = orderNumber;
        this.channel = channel;
        this.status = OrderStatus.CREATED;
        this.totalAmount = BigDecimal.ZERO;
    }

    // Helper method to maintain bidirectional relationship
    public void addItem(Product product, int quantity, BigDecimal unitPrice) {
        OrderItem item = new OrderItem(this, product, quantity, unitPrice);
        this.items.add(item);
        recalculateTotal();
    }

    public void recalculateTotal() {
        this.totalAmount = this.items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void transitionTo(OrderStatus newStatus) {
        // Enforce valid lifecycle transitions
        if (!isValidTransition(this.status, newStatus)) {
            throw new IllegalStateException("Invalid order state transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case CREATED -> next == OrderStatus.RESERVED || next == OrderStatus.CANCELLED;
            case RESERVED -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PACKING || next == OrderStatus.CANCELLED;
            case PACKING -> next == OrderStatus.DISPATCHED;
            case DISPATCHED -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false; // Terminal states
        };
    }
}