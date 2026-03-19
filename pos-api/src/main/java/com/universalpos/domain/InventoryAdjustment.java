package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * An immutable record of every stock quantity change.
 *
 * Every time stock moves — sale, return, PO receipt, manual correction,
 * damage write-off, theft — an adjustment record is created.
 * This gives a complete audit trail of inventory history for any product.
 *
 * This is append-only. Records are never updated or deleted.
 */
@Entity
@Table(name = "INVENTORY_ADJUSTMENTS")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inv_adj_seq")
    @SequenceGenerator(name = "inv_adj_seq", sequenceName = "INV_ADJ_SEQ", allocationSize = 1)
    @Column(name = "ADJ_ID")
    private Long adjId;

    @Column(name = "TENANT_ID", nullable = false)
    private Long tenantId;

    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;

    @Column(name = "EMPLOYEE_ID", nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ADJUSTMENT_TYPE", nullable = false, length = 30)
    private AdjustmentType adjustmentType;

    /** Stock quantity before this change */
    @Column(name = "QTY_BEFORE", nullable = false)
    private Integer qtyBefore;

    /** Positive = stock added, Negative = stock removed */
    @Column(name = "QTY_CHANGE", nullable = false)
    private Integer qtyChange;

    /** Stock quantity after this change (qtyBefore + qtyChange) */
    @Column(name = "QTY_AFTER", nullable = false)
    private Integer qtyAfter;

    @Column(name = "REASON", length = 500)
    private String reason;

    /** Optional reference to the related record (txnId, poId, countId) */
    @Column(name = "REFERENCE_ID")
    private Long referenceId;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AdjustmentType {
        SALE,             // Stock reduced by a sale transaction
        RETURN,           // Stock restored by a customer return
        PO_RECEIPT,       // Stock added when a purchase order is received
        MANUAL_ADD,       // Manager manually added stock (found extra units)
        MANUAL_REMOVE,    // Manager manually removed stock
        DAMAGE,           // Units written off as damaged
        THEFT,            // Units written off as stolen/shrinkage
        EXPIRY,           // Units written off as expired (food/perishables)
        TRANSFER,         // Stock moved between locations
        COUNT_CORRECTION  // Variance corrected after a physical stock count
    }
}
