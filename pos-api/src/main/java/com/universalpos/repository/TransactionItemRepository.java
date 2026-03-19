package com.universalpos.repository;

import com.universalpos.domain.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionItemRepository extends JpaRepository<TransactionItem, Long> {

    List<TransactionItem> findByTransaction_TxnId(Long txnId);

    /**
     * Top products by total revenue in a date range.
     * Returns: [productId, productName, sku, totalQty, totalRevenue]
     */
    @Query("SELECT ti.product.productId, ti.product.name, ti.product.sku, " +
           "SUM(ti.qty), SUM(ti.lineTotal) " +
           "FROM TransactionItem ti " +
           "WHERE ti.transaction.tenant.tenantId = :tenantId " +
           "AND ti.transaction.status = com.universalpos.domain.Transaction.TransactionStatus.COMPLETED " +
           "AND ti.transaction.txnType = com.universalpos.domain.Transaction.TransactionType.SALE " +
           "AND ti.transaction.createdAt BETWEEN :start AND :end " +
           "GROUP BY ti.product.productId, ti.product.name, ti.product.sku " +
           "ORDER BY SUM(ti.lineTotal) DESC")
    List<Object[]> topProductsByRevenue(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Top products by units sold in a date range.
     * Returns: [productId, productName, sku, totalQty, totalRevenue]
     */
    @Query("SELECT ti.product.productId, ti.product.name, ti.product.sku, " +
           "SUM(ti.qty), SUM(ti.lineTotal) " +
           "FROM TransactionItem ti " +
           "WHERE ti.transaction.tenant.tenantId = :tenantId " +
           "AND ti.transaction.status = com.universalpos.domain.Transaction.TransactionStatus.COMPLETED " +
           "AND ti.transaction.txnType = com.universalpos.domain.Transaction.TransactionType.SALE " +
           "AND ti.transaction.createdAt BETWEEN :start AND :end " +
           "GROUP BY ti.product.productId, ti.product.name, ti.product.sku " +
           "ORDER BY SUM(ti.qty) DESC")
    List<Object[]> topProductsByUnits(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
