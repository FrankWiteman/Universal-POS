package com.universalpos.repository;

import com.universalpos.domain.ReturnReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnReasonRepository extends JpaRepository<ReturnReason, Long> {

    List<ReturnReason> findByTenant_TenantIdAndActiveTrueOrderBySortOrderAsc(Long tenantId);
}
