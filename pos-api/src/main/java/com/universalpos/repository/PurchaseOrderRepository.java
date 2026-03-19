package com.universalpos.repository;

import com.universalpos.domain.PurchaseOrder;
import com.universalpos.domain.PurchaseOrder.PoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByPoNumberAndTenant_TenantId(String poNumber, Long tenantId);

    Page<PurchaseOrder> findByTenant_TenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    List<PurchaseOrder> findByTenant_TenantIdAndStatusOrderByCreatedAtDesc(
            Long tenantId, PoStatus status);

    @Query("SELECT COUNT(p) FROM PurchaseOrder p " +
           "WHERE p.tenant.tenantId = :tenantId " +
           "AND p.status IN ('SUBMITTED', 'CONFIRMED', 'PARTIAL')")
    long countOpenOrders(@Param("tenantId") Long tenantId);
}
