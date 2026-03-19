package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A customer belonging to a specific tenant.
 *
 * Loyalty tiers drive automatic discount eligibility:
 *   NONE → BRONZE (500 pts) → SILVER (1500 pts) → GOLD (5000 pts) → PLATINUM (10000 pts)
 */
@Entity
@Table(name = "CUSTOMERS",
       uniqueConstraints = {
           @UniqueConstraint(name = "UQ_CUSTOMER_EMAIL_TENANT",
                            columnNames = {"EMAIL", "TENANT_ID"}),
           @UniqueConstraint(name = "UQ_LOYALTY_CARD",
                            columnNames = {"LOYALTY_CARD_NUMBER"})
       })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
    @SequenceGenerator(name = "customer_seq", sequenceName = "CUSTOMER_SEQ", allocationSize = 1)
    @Column(name = "CUSTOMER_ID")
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TENANT_ID", nullable = false)
    private Tenant tenant;

    @Column(name = "FIRST_NAME", nullable = false, length = 60)
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false, length = 60)
    private String lastName;

    @Column(name = "EMAIL", length = 150)
    private String email;

    @Column(name = "PHONE", length = 20)
    private String phone;

    @Column(name = "LOYALTY_CARD_NUMBER", length = 20, unique = true)
    private String loyaltyCardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "LOYALTY_TIER", nullable = false, length = 20)
    @Builder.Default
    private LoyaltyTier loyaltyTier = LoyaltyTier.NONE;

    @Column(name = "LOYALTY_POINTS", nullable = false)
    @Builder.Default
    private Integer loyaltyPoints = 0;

    @Column(name = "DATE_OF_BIRTH")
    private LocalDate dateOfBirth;

    /** Opt-in for email marketing and receipts */
    @Column(name = "EMAIL_OPT_IN", nullable = false)
    @Builder.Default
    private Boolean emailOptIn = false;

    @Column(name = "NOTES", length = 500)
    private String notes;

    @Column(name = "ACTIVE", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    // ── Relationships ────────────────────────────────────────────
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    // ── Derived helpers ──────────────────────────────────────────

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Returns true if this customer should be shown as a VIP
     * (Gold or Platinum tier).
     */
    public boolean isVip() {
        return loyaltyTier == LoyaltyTier.GOLD || loyaltyTier == LoyaltyTier.PLATINUM;
    }

    /**
     * Loyalty tier enum with point thresholds.
     */
    public enum LoyaltyTier {
        NONE(0),
        BRONZE(500),
        SILVER(1500),
        GOLD(5000),
        PLATINUM(10000);

        private final int pointThreshold;

        LoyaltyTier(int pointThreshold) {
            this.pointThreshold = pointThreshold;
        }

        public int getPointThreshold() {
            return pointThreshold;
        }

        /** Determine tier from total lifetime points */
        public static LoyaltyTier fromPoints(int points) {
            LoyaltyTier result = NONE;
            for (LoyaltyTier tier : values()) {
                if (points >= tier.pointThreshold) {
                    result = tier;
                }
            }
            return result;
        }
    }
}
