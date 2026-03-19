package com.universalpos.repository;

import com.universalpos.domain.StockCount;
import com.universalpos.domain.StockCount.CountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockCountRepository extends JpaRepository<StockCount, Long> {

    Page<StockCount> findByTenant_TenantIdOrderByStartedAtDesc(Long tenantId, Pageable pageable);

    Optional<StockCount> findByTenant_TenantIdAndStatus(Long tenantId, CountStatus status);

    List<StockCount> findByTenant_TenantIdAndStatusOrderByStartedAtDesc(
            Long tenantId, CountStatus status);
}
