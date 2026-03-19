package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Receipt delivery record for a transaction.
 * Tracks whether an email was sent and/or a PDF was printed.
 */
@Entity
@Table(name = "RECEIPTS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "receipt_seq")
    @SequenceGenerator(name = "receipt_seq", sequenceName = "RECEIPT_SEQ", allocationSize = 1)
    @Column(name = "RECEIPT_ID")
    private Long receiptId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TXN_ID", nullable = false, unique = true)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TENANT_ID", nullable = false)
    private Tenant tenant;

    @Column(name = "EMAIL_ADDRESS", length = 150)
    private String emailAddress;

    @Column(name = "EMAILED", nullable = false)
    @Builder.Default
    private Boolean emailed = false;

    @Column(name = "EMAILED_AT")
    private LocalDateTime emailedAt;

    @Column(name = "PRINTED", nullable = false)
    @Builder.Default
    private Boolean printed = false;

    @Column(name = "PRINTED_AT")
    private LocalDateTime printedAt;

    /** Path to generated PDF on disk */
    @Column(name = "PDF_PATH", length = 500)
    private String pdfPath;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
