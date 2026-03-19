package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One product line within a stock count session.
 * Records what the system expected vs what staff physically counted.
 */
@Entity
@Table(name = "STOCK_COUNT_ITEMS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCountItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sc_item_seq")
    @SequenceGenerator(name = "sc_item_seq", sequenceName = "STOCK_COUNT_ITEM_SEQ", allocationSize = 1)
    @Column(name = "ITEM_ID")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COUNT_ID", nullable = false)
    private StockCount stockCount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    /** What the system said the stock level was at count start */
    @Column(name = "QTY_EXPECTED")
    private Integer qtyExpected;

    /** What staff physically counted — null until entered */
    @Column(name = "QTY_COUNTED")
    private Integer qtyCounted;

    /** qtyCounted - qtyExpected. Negative = shrinkage, Positive = surplus */
    @Column(name = "VARIANCE")
    private Integer variance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COUNTED_BY")
    private Employee countedBy;

    @Column(name = "COUNTED_AT")
    private LocalDateTime countedAt;

    // ── Helpers ──────────────────────────────────────────────────

    /** Call this when staff enters their counted quantity */
    public void recordCount(Integer counted, Employee counter) {
        this.qtyCounted  = counted;
        this.variance    = (qtyExpected != null) ? counted - qtyExpected : null;
        this.countedBy   = counter;
        this.countedAt   = LocalDateTime.now();
    }

    public boolean hasVariance() {
        return variance != null && variance != 0;
    }
}
