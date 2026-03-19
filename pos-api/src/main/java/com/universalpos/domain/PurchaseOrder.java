package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A purchase order sent to a supplier to restock inventory.
 *
 * Lifecycle:
 *   DRAFT → SUBMITTED → CONFIRMED → PARTIAL (some received) → RECEIVED
 *   Any state can go to CANCELLED
 *
 * When status moves to RECEIVED (or PARTIAL), inventory quantities
 * are updated and INVENTORY_ADJUSTMENTS records are written.
 */
@Entity
@Table(name = "PURCHASE_ORDERS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_seq")
    @SequenceGenerator(name = "po_seq", sequenceName = "PO_SEQ", allocationSize = 1)
    @Column(name = "PO_ID")
    private Long poId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TENANT_ID", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "SUPPLIER_ID", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREATED_BY", nullable = false)
    private Employee createdBy;

    /** Human-readable PO number e.g. "PO-20240317-001" */
    @Column(name = "PO_NUMBER", nullable = false, unique = true, length = 30)
    private String poNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private PoStatus status = PoStatus.DRAFT;

    @Column(name = "ORDER_DATE")
    private LocalDate orderDate;

    @Column(name = "EXPECTED_DATE")
    private LocalDate expectedDate;

    @Column(name = "RECEIVED_DATE")
    private LocalDate receivedDate;

    @Column(name = "SUBTOTAL", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "TAX_AMOUNT", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "SHIPPING_COST", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "TOTAL", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "NOTES", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();

    // ── Helpers ──────────────────────────────────────────────────

    public boolean isEditable() {
        return status == PoStatus.DRAFT;
    }

    public boolean canReceive() {
        return status == PoStatus.SUBMITTED
            || status == PoStatus.CONFIRMED
            || status == PoStatus.PARTIAL;
    }

    public enum PoStatus {
        DRAFT,      // Being built — not yet sent to supplier
        SUBMITTED,  // Sent to supplier, awaiting confirmation
        CONFIRMED,  // Supplier confirmed the order
        PARTIAL,    // Some items received, waiting on rest
        RECEIVED,   // All items received — PO complete
        CANCELLED
    }
}
