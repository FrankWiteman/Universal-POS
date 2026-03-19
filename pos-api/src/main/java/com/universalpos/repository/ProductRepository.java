package com.universalpos.repository;

import com.universalpos.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcodeAndTenant_TenantIdAndActiveTrue(
            String barcode, Long tenantId);

    Optional<Product> findBySkuAndTenant_TenantIdAndActiveTrue(
            String sku, Long tenantId);

    /**
     * Full-text style search across name, SKU, brand, and barcode.
     * Used by the POS terminal product search panel.
     */
    @Query("SELECT p FROM Product p WHERE p.tenant.tenantId = :tenantId " +
           "AND p.active = true " +
           "AND (LOWER(p.name)    LIKE LOWER(CONCAT('%', :term, '%')) " +
           " OR  LOWER(p.sku)     LIKE LOWER(CONCAT('%', :term, '%')) " +
           " OR  LOWER(p.brand)   LIKE LOWER(CONCAT('%', :term, '%')) " +
           " OR  p.barcode = :term)")
    Page<Product> searchProducts(
            @Param("tenantId") Long tenantId,
            @Param("term") String term,
            Pageable pageable);
}
