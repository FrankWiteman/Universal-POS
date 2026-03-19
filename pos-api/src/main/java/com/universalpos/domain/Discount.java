package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A configurable discount rule for a tenant.
 *
 * The discount engine evaluates all active rules against a cart
 * and customer and applies the best eligible one(s).
 *
 * Types:
 *   PERCENT        — e.g. 10% off entire cart
 *   FIXED_AMOUNT   — e.g. $5 off orders over $50
 *   LOYALTY_TIER   — auto-applied to customers of a certain tier
 *   COUPON_CODE    — requires a code entered at checkout
 *   EMPLOYEE       — available only to employees (with manager override)
 */
@Entity
@Table(name = "DISCOUNTS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "discount_seq")
    @SequenceGenerator(name = "discount_seq", sequenceName = "DISCOUNT_SEQ", allocationSize = 1)
    @Column(name = "DISCOUNT_ID")
    private Long discountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TENANT_ID", nullable = false)
    private Tenant tenant;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "DESCRIPTION", length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "DISCOUNT_TYPE", nullable = false, length = 30)
    private DiscountType discountType;

    /** The discount value — percent (0-100) or fixed dollar amount */
    @Column(name = "VALUE", nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    /** Minimum cart subtotal to qualify for this discount */
    @Column(name = "MIN_PURCHASE", precision = 10, scale = 2)
    private BigDecimal minPurchase;

    /** If set, only customers at this tier or above qualify */
    @Enumerated(EnumType.STRING)
    @Column(name = "LOYALTY_TIER_REQUIRED", length = 20)
    private Customer.LoyaltyTier loyaltyTierRequired;

    /** If set, customer must enter this code at checkout */
    @Column(name = "COUPON_CODE", length = 30)
    private String couponCode;

    /** Max times this discount can be used (null = unlimited) */
    @Column(name = "MAX_USES")
    private Integer maxUses;

    @Column(name = "TIMES_USED", nullable = false)
    @Builder.Default
    private Integer timesUsed = 0;

    @Column(name = "START_DATE")
    private LocalDate startDate;

    @Column(name = "END_DATE")
    private LocalDate endDate;

    /** Manager must be logged in to apply this discount */
    @Column(name = "REQUIRES_MANAGER", nullable = false)
    @Builder.Default
    private Boolean requiresManager = false;

    @Column(name = "ACTIVE", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Helpers ──────────────────────────────────────────────────

    public boolean isCurrentlyValid() {
        LocalDate today = LocalDate.now();
        boolean afterStart = startDate == null || !today.isBefore(startDate);
        boolean beforeEnd  = endDate   == null || !today.isAfter(endDate);
        boolean hasUsesLeft = maxUses == null || timesUsed < maxUses;
        return active && afterStart && beforeEnd && hasUsesLeft;
    }

    public enum DiscountType {
        PERCENT,
        FIXED_AMOUNT,
        LOYALTY_TIER,
        COUPON_CODE,
        EMPLOYEE
    }
}
