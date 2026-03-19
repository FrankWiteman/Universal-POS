package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A supplier/vendor that the store orders inventory from.
 * Examples: Fender distributor, a local wholesale rep, Amazon Business account.
 */
@Entity
@Table(name = "SUPPLIERS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "supplier_seq")
    @SequenceGenerator(name = "supplier_seq", sequenceName = "SUPPLIER_SEQ", allocationSize = 1)
    @Column(name = "SUPPLIER_ID")
    private Long supplierId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TENANT_ID", nullable = false)
    private Tenant tenant;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "CONTACT_NAME", length = 120)
    private String contactName;

    @Column(name = "EMAIL", length = 150)
    private String email;

    @Column(name = "PHONE", length = 30)
    private String phone;

    @Column(name = "ADDRESS", length = 300)
    private String address;

    @Column(name = "WEBSITE", length = 200)
    private String website;

    /** Your account number with this supplier */
    @Column(name = "ACCOUNT_NUMBER", length = 50)
    private String accountNumber;

    /** e.g. "Net 30", "COD", "2/10 Net 30" */
    @Column(name = "PAYMENT_TERMS", length = 100)
    private String paymentTerms;

    /** Average days from order to delivery */
    @Column(name = "LEAD_TIME_DAYS")
    @Builder.Default
    private Integer leadTimeDays = 7;

    @Column(name = "NOTES", length = 1000)
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

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private List<PurchaseOrder> purchaseOrders;
}
