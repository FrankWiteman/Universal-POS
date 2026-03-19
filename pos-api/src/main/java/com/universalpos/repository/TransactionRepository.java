package com.universalpos.repository;

import com.universalpos.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReceiptNumberAndTenant_TenantId(
            String receiptNumber, Long tenantId);

    Page<Transaction> findByCustomer_CustomerIdAndTenant_TenantIdOrderByCreatedAtDesc(
            Long customerId, Long tenantId, Pageable pageable);

    /** Daily sales total for manager reports */
    @Query("SELECT COALESCE(SUM(t.total), 0) FROM Transaction t " +
           "WHERE t.tenant.tenantId = :tenantId " +
           "AND t.status = 'COMPLETED' " +
           "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumCompletedTotalsBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.tenant.tenantId = :tenantId " +
           "AND t.status = 'COMPLETED' " +
           "AND t.createdAt BETWEEN :start AND :end")
    Long countCompletedBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
