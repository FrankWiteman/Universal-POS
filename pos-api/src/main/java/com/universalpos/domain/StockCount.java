package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A physical inventory count session.
 *
 * Types:
 *   FULL     — count every product in the store
 *   PARTIAL  — count a specific list of products
 *   CATEGORY — count all products in a category (e.g. "Guitars")
 *
 * Workflow:
 *   1. Manager creates a StockCount (status = IN_PROGRESS)
 *   2. Staff count physical units and enter QTY_COUNTED per item
 *   3. System shows variances (expected vs counted)
 *   4. Manager reviews and approves → status = COMPLETED
 *   5. System applies COUNT_CORRECTION adjustments for any variances
 */
@Entity
@Table(name = "STOCK_COUNTS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCount {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stock_count_seq")
    @SequenceGenerator(name = "stock_count_seq", sequenceName = "STOCK_COUNT_SEQ", allocationSize = 1)
    @Column(name = "COUNT_ID")
    private Long countId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TENANT_ID", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREATED_BY", nullable = false)
    private Employee createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private CountStatus status = CountStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    @Column(name = "COUNT_TYPE", nullable = false, length = 20)
    @Builder.Default
    private CountType countType = CountType.FULL;

    /** For CATEGORY counts — which category to count */
    @Column(name = "CATEGORY_FILTER", length = 100)
    private String categoryFilter;

    @CreationTimestamp
    @Column(name = "STARTED_AT", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;

    @Column(name = "NOTES", length = 500)
    private String notes;

    @OneToMany(mappedBy = "stockCount", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockCountItem> items = new ArrayList<>();

    public enum CountStatus { IN_PROGRESS, COMPLETED, CANCELLED }
    public enum CountType   { FULL, PARTIAL, CATEGORY }
}
