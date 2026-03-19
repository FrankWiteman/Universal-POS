package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A completed (or voided) POS transaction.
 *
 * Immutable once status = COMPLETED.
 * Void creates a new VOID transaction linked back here.
 */
@Entity
@Table(name = "TRANSACTIONS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "txn_seq")
    @SequenceGenerator(name = "txn_seq", sequenceName = "TRANSACTION_SEQ", allocationSize = 1)
    @Column(name = "TXN_ID")
    private Long txnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TENANT_ID", nullable = false)
    private Tenant tenant;

    /** Nullable — transaction can occur without a loyalty customer on file */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMPLOYEE_ID", nullable = false)
    private Employee employee;

    /** Human-readable receipt number, e.g. "TXN-20240317-0042" */
    @Column(name = "RECEIPT_NUMBER", nullable = false, unique = true, length = 30)
    private String receiptNumber;

    @Column(name = "SUBTOTAL", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "DISCOUNT_AMOUNT", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "TAX_AMOUNT", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "TOTAL", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_METHOD", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "AMOUNT_TENDERED", precision = 10, scale = 2)
    private BigDecimal amountTendered;

    @Column(name = "CHANGE_DUE", precision = 10, scale = 2)
    private BigDecimal changeDue;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    @Column(name = "TXN_TYPE", nullable = false, length = 20)
    @Builder.Default
    private TransactionType txnType = TransactionType.SALE;

    /** For returns/voids — links back to the original transaction */
    @Column(name = "ORIGINAL_TXN_ID")
    private Long originalTxnId;

    /** How many loyalty points were earned on this transaction */
    @Column(name = "LOYALTY_POINTS_EARNED")
    @Builder.Default
    private Integer loyaltyPointsEarned = 0;

    @Column(name = "NOTES", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;

    // ── Relationships ────────────────────────────────────────────
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TransactionItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Receipt receipt;

    // ── Enums ────────────────────────────────────────────────────

    public enum TransactionStatus {
        IN_PROGRESS,
        COMPLETED,
        VOIDED,
        REFUNDED
    }

    public enum TransactionType {
        SALE,
        RETURN,
        EXCHANGE,
        VOID
    }

    public enum PaymentMethod {
        CASH,
        CREDIT_CARD,
        DEBIT_CARD,
        GIFT_CARD,
        SPLIT
    }
}
