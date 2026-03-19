package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Tracks exactly which item(s) were returned in a return transaction.
 *
 * One row per product line.
 * Links the return transaction back to the specific original line item.
 *
 * RESTOCK = true  → product stock is incremented (default)
 * RESTOCK = false → item is damaged/unsellable, do NOT restock
 */
@Entity
@Table(name = "RETURN_ITEMS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "return_item_seq")
    @SequenceGenerator(name = "return_item_seq", sequenceName = "RETURN_ITEM_SEQ", allocationSize = 1)
    @Column(name = "RETURN_ITEM_ID")
    private Long returnItemId;

    /** The RETURN or EXCHANGE transaction this belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RETURN_TXN_ID", nullable = false)
    private Transaction returnTransaction;

    /** The original SALE transaction */
    @Column(name = "ORIGINAL_TXN_ID", nullable = false)
    private Long originalTxnId;

    /** The specific line item in the original sale */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ORIGINAL_ITEM_ID", nullable = false)
    private TransactionItem originalItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    @Column(name = "QTY_RETURNED", nullable = false)
    private Integer qtyReturned;

    /** Price at time of original sale — used to calculate refund */
    @Column(name = "UNIT_PRICE", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** Actual refund amount for this line (may be less than full price) */
    @Column(name = "REFUND_AMOUNT", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "REASON_ID")
    private ReturnReason reason;

    /** Should this item go back into sellable stock? */
    @Column(name = "RESTOCK", nullable = false)
    @Builder.Default
    private Boolean restock = true;
}
