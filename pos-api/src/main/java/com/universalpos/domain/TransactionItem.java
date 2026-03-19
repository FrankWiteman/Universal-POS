package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A single line item within a transaction (one product, one or more units).
 */
@Entity
@Table(name = "TRANSACTION_ITEMS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "txn_item_seq")
    @SequenceGenerator(name = "txn_item_seq", sequenceName = "TXN_ITEM_SEQ", allocationSize = 1)
    @Column(name = "ITEM_ID")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TXN_ID", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    @Column(name = "QTY", nullable = false)
    private Integer qty;

    /** Price at time of sale — snapshot, not live product price */
    @Column(name = "UNIT_PRICE", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "DISCOUNT_APPLIED", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountApplied = BigDecimal.ZERO;

    @Column(name = "LINE_TOTAL", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    /** Name of the discount rule applied, for receipt display */
    @Column(name = "DISCOUNT_LABEL", length = 100)
    private String discountLabel;
}
