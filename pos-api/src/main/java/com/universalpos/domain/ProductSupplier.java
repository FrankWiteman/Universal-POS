package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Links a product to a supplier with supplier-specific details.
 * A product can have multiple suppliers; preferred = true marks the default.
 */
@Entity
@Table(name = "PRODUCT_SUPPLIERS")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prod_supp_seq")
    @SequenceGenerator(name = "prod_supp_seq", sequenceName = "PROD_SUPP_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "SUPPLIER_ID", nullable = false)
    private Supplier supplier;

    /** The SKU/part number this supplier uses for this product */
    @Column(name = "SUPPLIER_SKU", length = 100)
    private String supplierSku;

    /** What you pay the supplier per unit */
    @Column(name = "UNIT_COST", precision = 10, scale = 2)
    private BigDecimal unitCost;

    /** Minimum quantity required per order */
    @Column(name = "MIN_ORDER_QTY")
    @Builder.Default
    private Integer minOrderQty = 1;

    /** Is this the default/preferred supplier for this product? */
    @Column(name = "PREFERRED", nullable = false)
    @Builder.Default
    private Boolean preferred = false;

    @Column(name = "ACTIVE", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
