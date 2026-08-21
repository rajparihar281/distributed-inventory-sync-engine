package com.inventory.sync.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class InventoryAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private InventoryAuditType changeType;

    @Column(name = "delta_physical", nullable = false)
    private Integer deltaPhysical = 0;

    @Column(name = "delta_available", nullable = false)
    private Integer deltaAvailable = 0;

    @Column(name = "delta_reserved", nullable = false)
    private Integer deltaReserved = 0;

    @Column(name = "delta_damaged", nullable = false)
    private Integer deltaDamaged = 0;

    @Column(name = "reference_id", length = 64)
    private String referenceId;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public InventoryAuditLog(Inventory inventory, InventoryAuditType changeType,
                             int deltaPhysical, int deltaAvailable, int deltaReserved, int deltaDamaged,
                             String referenceId, String reason) {
        this.inventory = inventory;
        this.changeType = changeType;
        this.deltaPhysical = deltaPhysical;
        this.deltaAvailable = deltaAvailable;
        this.deltaReserved = deltaReserved;
        this.deltaDamaged = deltaDamaged;
        this.referenceId = referenceId;
        this.reason = reason;
    }
}