package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One line item on a purchase order — one product, ordered quantity, and cost.
 */
@Entity
@Table(name = "PURCHASE_ORDER_ITEMS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_item_seq")
    @SequenceGenerator(name = "po_item_seq", sequenceName = "PO_ITEM_SEQ", allocationSize = 1)
    @Column(name = "ITEM_ID")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PO_ID", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    @Column(name = "QTY_ORDERED", nullable = false)
    private Integer qtyOrdered;

    /** Updated incrementally as shipments arrive */
    @Column(name = "QTY_RECEIVED", nullable = false)
    @Builder.Default
    private Integer qtyReceived = 0;

    @Column(name = "UNIT_COST", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "LINE_TOTAL", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "NOTES", length = 500)
    private String notes;

    // ── Helpers ──────────────────────────────────────────────────

    public Integer getQtyOutstanding() {
        return qtyOrdered - qtyReceived;
    }

    public boolean isFullyReceived() {
        return qtyReceived >= qtyOrdered;
    }
}
