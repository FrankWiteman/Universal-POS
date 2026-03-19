package com.universalpos.repository;

import com.universalpos.domain.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByTenant_TenantIdAndActiveTrueOrderByNameAsc(Long tenantId);

    @Query("SELECT s FROM Supplier s WHERE s.tenant.tenantId = :tenantId " +
           "AND s.active = true " +
           "AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :term, '%')) " +
           " OR  LOWER(s.contactName) LIKE LOWER(CONCAT('%', :term, '%')))")
    Page<Supplier> searchSuppliers(@Param("tenantId") Long tenantId,
                                   @Param("term") String term,
                                   Pageable pageable);
}
