package com.universalpos.repository;

import com.universalpos.domain.InventoryAdjustment;
import com.universalpos.domain.InventoryAdjustment.AdjustmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {

    Page<InventoryAdjustment> findByTenantIdAndProductIdOrderByCreatedAtDesc(
            Long tenantId, Long productId, Pageable pageable);

    Page<InventoryAdjustment> findByTenantIdOrderByCreatedAtDesc(
            Long tenantId, Pageable pageable);

    /**
     * Shrinkage report — units lost by type (DAMAGE, THEFT, EXPIRY, etc.)
     * Returns: [adjustmentType, productId, productName, totalUnitsLost]
     */
    @Query("SELECT ia.adjustmentType, ia.productId, " +
           "COALESCE(SUM(ABS(ia.qtyChange)), 0) " +
           "FROM InventoryAdjustment ia " +
           "WHERE ia.tenantId = :tenantId " +
           "AND ia.adjustmentType IN :types " +
           "AND ia.createdAt BETWEEN :start AND :end " +
           "GROUP BY ia.adjustmentType, ia.productId " +
           "ORDER BY SUM(ABS(ia.qtyChange)) DESC")
    List<Object[]> shrinkageByType(
            @Param("tenantId") Long tenantId,
            @Param("types") List<AdjustmentType> types,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
