package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * A configurable reason code for returns.
 * Shown to cashiers as a dropdown when processing a return.
 *
 * Examples: DEFECTIVE, WRONG_ITEM, CHANGED_MIND, DUPLICATE_ORDER,
 *           DAMAGED_SHIPPING, NOT_AS_DESCRIBED
 *
 * REQUIRES_MANAGER = true means this reason requires manager approval
 * (e.g. "POLICY_EXCEPTION" or "NO_RECEIPT").
 */
@Entity
@Table(name = "RETURN_REASONS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnReason {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "return_reason_seq")
    @SequenceGenerator(name = "return_reason_seq", sequenceName = "RETURN_REASON_SEQ", allocationSize = 1)
    @Column(name = "REASON_ID")
    private Long reasonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TENANT_ID", nullable = false)
    private Tenant tenant;

    /** Short code used in reports, e.g. "DEFECTIVE", "WRONG_ITEM" */
    @Column(name = "CODE", nullable = false, length = 30)
    private String code;

    /** Human-readable label shown to cashier, e.g. "Item was defective" */
    @Column(name = "DESCRIPTION", nullable = false, length = 200)
    private String description;

    /** If true, a manager must be present to use this reason */
    @Column(name = "REQUIRES_MANAGER", nullable = false)
    @Builder.Default
    private Boolean requiresManager = false;

    @Column(name = "ACTIVE", nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Controls display order in the cashier dropdown */
    @Column(name = "SORT_ORDER")
    @Builder.Default
    private Integer sortOrder = 0;
}
