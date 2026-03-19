package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a company/store using the POS system.
 * Every piece of data is scoped to a Tenant — this is the root of multi-tenancy.
 *
 * Example tenants: "Guitar Center #0042", "Main Street Music", "TechWave Electronics"
 */
@Entity
@Table(name = "TENANTS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_seq")
    @SequenceGenerator(name = "tenant_seq", sequenceName = "TENANT_SEQ", allocationSize = 1)
    @Column(name = "TENANT_ID")
    private Long tenantId;

    @Column(name = "COMPANY_NAME", nullable = false, length = 100)
    private String companyName;

    /** Slug used in API paths and config lookups, e.g. "guitar-center" */
    @Column(name = "TENANT_SLUG", nullable = false, unique = true, length = 50)
    private String tenantSlug;

    @Column(name = "LOGO_URL", length = 500)
    private String logoUrl;

    /** Hex color for receipt/UI branding, e.g. "#C8102E" */
    @Column(name = "BRAND_COLOR", length = 7)
    private String brandColor;

    @Column(name = "RECEIPT_HEADER", length = 200)
    private String receiptHeader;

    @Column(name = "RECEIPT_FOOTER", length = 200)
    private String receiptFooter;

    /** Tax rate as a decimal, e.g. 0.0825 = 8.25% */
    @Column(name = "TAX_RATE", nullable = false)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Builder.Default
    private Double taxRate = 0.0825;

    @Column(name = "CURRENCY_CODE", length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Column(name = "TIMEZONE", length = 50)
    @Builder.Default
    private String timezone = "America/Chicago";

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
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Employee> employees;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Customer> customers;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;
}
