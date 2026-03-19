package com.universalpos.repository;

import com.universalpos.domain.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {

    /**
     * Find all currently valid discounts for a tenant —
     * active, within date range, and with uses remaining.
     */
    @Query("SELECT d FROM Discount d WHERE d.tenant.tenantId = :tenantId " +
           "AND d.active = true " +
           "AND (d.startDate IS NULL OR d.startDate <= :today) " +
           "AND (d.endDate   IS NULL OR d.endDate   >= :today) " +
           "AND (d.maxUses   IS NULL OR d.timesUsed < d.maxUses)")
    List<Discount> findValidDiscounts(
            @Param("tenantId") Long tenantId,
            @Param("today") LocalDate today);

    Optional<Discount> findByCouponCodeAndTenant_TenantIdAndActiveTrue(
            String couponCode, Long tenantId);
}
