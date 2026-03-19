package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A product/item in a tenant's catalog.
 *
 * BigDecimal is used for all monetary values — never use float/double for money.
 */
@Entity
@Table(name = "PRODUCTS",
       uniqueConstraints = {
           @UniqueConstraint(name = "UQ_PRODUCT_SKU_TENANT",
                            columnNames = {"SKU", "TENANT_ID"})
       })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(name = "product_seq", sequenceName = "PRODUCT_SEQ", allocationSize = 1)
    @Column(name = "PRODUCT_ID")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TENANT_ID", nullable = false)
    private Tenant tenant;

    @Column(name = "SKU", nullable = false, length = 50)
    private String sku;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    /** Retail price — always BigDecimal for monetary values */
    @Column(name = "PRICE", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Cost price for margin reporting */
    @Column(name = "COST", precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "CATEGORY", length = 100)
    private String category;

    @Column(name = "SUBCATEGORY", length = 100)
    private String subcategory;

    @Column(name = "BRAND", length = 100)
    private String brand;

    /** UPC/EAN barcode for scanner input */
    @Column(name = "BARCODE", length = 50)
    private String barcode;

    @Column(name = "STOCK_QTY", nullable = false)
    @Builder.Default
    private Integer stockQty = 0;

    /** Low stock alert threshold */
    @Column(name = "REORDER_POINT")
    @Builder.Default
    private Integer reorderPoint = 5;

    /** Whether this product is taxable */
    @Column(name = "TAXABLE", nullable = false)
    @Builder.Default
    private Boolean taxable = true;

    @Column(name = "ACTIVE", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "IMAGE_URL", length = 500)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    // ── Helpers ──────────────────────────────────────────────────

    public boolean isInStock() {
        return stockQty != null && stockQty > 0;
    }

    public boolean isLowStock() {
        return stockQty != null && reorderPoint != null && stockQty <= reorderPoint;
    }
}
