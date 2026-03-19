package com.universalpos.repository;

import com.universalpos.domain.InventoryAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {

    Page<InventoryAdjustment> findByTenantIdAndProductIdOrderByCreatedAtDesc(
            Long tenantId, Long productId, Pageable pageable);

    Page<InventoryAdjustment> findByTenantIdOrderByCreatedAtDesc(
            Long tenantId, Pageable pageable);
}
