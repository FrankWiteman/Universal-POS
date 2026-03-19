package com.universalpos.repository;

import com.universalpos.domain.Transaction;
import com.universalpos.domain.Transaction.TransactionStatus;
import com.universalpos.domain.Transaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReceiptNumberAndTenant_TenantId(
            String receiptNumber, Long tenantId);

    Page<Transaction> findByCustomer_CustomerIdAndTenant_TenantIdOrderByCreatedAtDesc(
            Long customerId, Long tenantId, Pageable pageable);

    Page<Transaction> findByTenant_TenantIdOrderByCreatedAtDesc(
            Long tenantId, Pageable pageable);

    // ── Sales totals ─────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(t.total), 0) FROM Transaction t " +
           "WHERE t.tenant.tenantId = :tenantId " +
           "AND t.status = com.universalpos.domain.Transaction.TransactionStatus.COMPLETED " +
           "AND t.txnType = com.universalpos.domain.Transaction.TransactionType.SALE " +
           "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumSalesTotalBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.tenant.tenantId = :tenantId " +
           "AND t.status = com.universalpos.domain.Transaction.TransactionStatus.COMPLETED " +
           "AND t.txnType = com.universalpos.domain.Transaction.TransactionType.SALE " +
           "AND t.createdAt BETWEEN :start AND :end")
    Long countSalesBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.discountAmount), 0) FROM Transaction t " +
           "WHERE t.tenant.tenantId = :tenantId " +
           "AND t.status = com.universalpos.domain.Transaction.TransactionStatus.COMPLETED " +
           "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumDiscountsBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.taxAmount), 0) FROM Transaction t " +
           "WHERE t.tenant.tenantId = :tenantId " +
           "AND t.status = com.universalpos.domain.Transaction.TransactionStatus.COMPLETED " +
           "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumTaxBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // ── Returns ──────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(ABS(t.total)), 0) FROM Transaction t " +
           "WHERE t.tenant.tenantId = :tenantId " +
           "AND t.txnType = com.universalpos.domain.Transaction.TransactionType.RETURN " +
           "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumReturnsTotalBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.tenant.tenantId = :tenantId " +
           "AND t.txnType = com.universalpos.domain.Transaction.TransactionType.RETURN " +
           "AND t.createdAt BETWEEN :start AND :end")
    Long countReturnsBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // ── Employee performance ──────────────────────────────────

    @Query("SELECT t.employee.employeeId, t.employee.firstName, t.employee.lastName, " +
           "COUNT(t), COALESCE(SUM(t.total), 0) " +
           "FROM Transaction t " +
           "WHERE t.tenant.tenantId = :tenantId " +
           "AND t.status = com.universalpos.domain.Transaction.TransactionStatus.COMPLETED " +
           "AND t.txnType = com.universalpos.domain.Transaction.TransactionType.SALE " +
           "AND t.createdAt BETWEEN :start AND :end " +
           "GROUP BY t.employee.employeeId, t.employee.firstName, t.employee.lastName " +
           "ORDER BY SUM(t.total) DESC")
    List<Object[]> employeePerformanceBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // ── Hourly breakdown ─────────────────────────────────────

    @Query("SELECT FUNCTION('TO_CHAR', t.createdAt, 'HH24'), " +
           "COUNT(t), COALESCE(SUM(t.total), 0) " +
           "FROM Transaction t " +
           "WHERE t.tenant.tenantId = :tenantId " +
           "AND t.status = com.universalpos.domain.Transaction.TransactionStatus.COMPLETED " +
           "AND t.createdAt BETWEEN :start AND :end " +
           "GROUP BY FUNCTION('TO_CHAR', t.createdAt, 'HH24') " +
           "ORDER BY FUNCTION('TO_CHAR', t.createdAt, 'HH24')")
    List<Object[]> hourlySalesBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Legacy (kept for backward compat)
    @Query("SELECT COALESCE(SUM(t.total), 0) FROM Transaction t " +
           "WHERE t.tenant.tenantId = :tenantId " +
           "AND t.status = com.universalpos.domain.Transaction.TransactionStatus.COMPLETED " +
           "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumCompletedTotalsBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.tenant.tenantId = :tenantId " +
           "AND t.status = com.universalpos.domain.Transaction.TransactionStatus.COMPLETED " +
           "AND t.createdAt BETWEEN :start AND :end")
    Long countCompletedBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
