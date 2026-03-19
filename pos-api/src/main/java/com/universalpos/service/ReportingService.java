package com.universalpos.service;

import com.universalpos.domain.InventoryAdjustment.AdjustmentType;
import com.universalpos.domain.Product;
import com.universalpos.dto.response.*;
import com.universalpos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportingService — Phase 3
 *
 * All reports are read-only, date-range scoped, and tenant-isolated.
 * No mutations — this service only reads data.
 *
 * Reports available:
 *   dailySales       — revenue, transactions, returns, hourly breakdown
 *   topProducts      — best sellers by revenue or units
 *   employeePerf     — per-cashier transaction count and revenue
 *   shrinkage        — units lost to damage/theft/expiry
 *   lowStock         — products at/below reorder point + suggested qty
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportingService {

    private final TransactionRepository        transactionRepository;
    private final TransactionItemRepository    transactionItemRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final ProductRepository            productRepository;
    private final SupplierRepository           supplierRepository;

    // ══════════════════════════════════════════════════════
    //  DAILY SALES REPORT
    // ══════════════════════════════════════════════════════

    public DailySalesReport dailySales(Long tenantId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(LocalTime.MAX);

        BigDecimal grossRevenue     = orZero(transactionRepository.sumSalesTotalBetween(tenantId, start, end));
        Long       totalTxns        = orZeroLong(transactionRepository.countSalesBetween(tenantId, start, end));
        BigDecimal totalDiscounts   = orZero(transactionRepository.sumDiscountsBetween(tenantId, start, end));
        BigDecimal totalTax         = orZero(transactionRepository.sumTaxBetween(tenantId, start, end));
        BigDecimal netRevenue       = grossRevenue.subtract(totalDiscounts);
        BigDecimal avgTicket        = totalTxns > 0
                ? grossRevenue.divide(BigDecimal.valueOf(totalTxns), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalRefunded    = orZero(transactionRepository.sumReturnsTotalBetween(tenantId, start, end));
        Long       totalReturns     = orZeroLong(transactionRepository.countReturnsBetween(tenantId, start, end));
        BigDecimal returnRate       = totalTxns > 0
                ? BigDecimal.valueOf(totalReturns)
                    .divide(BigDecimal.valueOf(totalTxns), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Hourly breakdown
        List<Object[]> hourlyRaw = transactionRepository.hourlySalesBetween(tenantId, start, end);
        List<DailySalesReport.HourlyBreakdown> byHour = hourlyRaw.stream()
                .map(row -> {
                    String hour = (String) row[0];
                    int h = Integer.parseInt(hour);
                    String label = h == 0 ? "12 AM" : h < 12 ? h + " AM"
                                 : h == 12 ? "12 PM" : (h - 12) + " PM";
                    return DailySalesReport.HourlyBreakdown.builder()
                            .hour(hour)
                            .hourLabel(label)
                            .transactions(((Number) row[1]).longValue())
                            .revenue(new BigDecimal(row[2].toString()).setScale(2, RoundingMode.HALF_UP))
                            .build();
                })
                .collect(Collectors.toList());

        log.info("Daily sales report: tenant={} from={} to={} revenue=${} txns={}",
                tenantId, from, to, grossRevenue, totalTxns);

        return DailySalesReport.builder()
                .from(from).to(to)
                .totalTransactions(totalTxns)
                .grossRevenue(grossRevenue)
                .totalDiscounts(totalDiscounts)
                .totalTax(totalTax)
                .netRevenue(netRevenue)
                .averageTicket(avgTicket)
                .totalReturns(totalReturns)
                .totalRefunded(totalRefunded)
                .returnRate(returnRate)
                .byHour(byHour)
                .build();
    }

    // ══════════════════════════════════════════════════════
    //  TOP PRODUCTS REPORT
    // ══════════════════════════════════════════════════════

    public TopProductsReport topProducts(Long tenantId, LocalDate from, LocalDate to,
                                          String sortBy, int limit) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(LocalTime.MAX);

        List<Object[]> raw = sortBy.equalsIgnoreCase("UNITS")
                ? transactionItemRepository.topProductsByUnits(tenantId, start, end)
                : transactionItemRepository.topProductsByRevenue(tenantId, start, end);

        List<TopProductsReport.ProductRow> rows = new ArrayList<>();
        int rank = 1;
        for (Object[] row : raw) {
            if (rank > limit) break;
            rows.add(TopProductsReport.ProductRow.builder()
                    .productId(((Number) row[0]).longValue())
                    .productName((String) row[1])
                    .sku((String) row[2])
                    .unitsSold(((Number) row[3]).longValue())
                    .revenue(new BigDecimal(row[4].toString()).setScale(2, RoundingMode.HALF_UP))
                    .rank(rank++)
                    .build());
        }

        return TopProductsReport.builder()
                .from(from).to(to)
                .sortedBy(sortBy.toUpperCase())
                .products(rows)
                .build();
    }

    // ══════════════════════════════════════════════════════
    //  EMPLOYEE PERFORMANCE REPORT
    // ══════════════════════════════════════════════════════

    public EmployeePerformanceReport employeePerformance(Long tenantId,
                                                          LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(LocalTime.MAX);

        List<Object[]> raw = transactionRepository.employeePerformanceBetween(tenantId, start, end);

        List<EmployeePerformanceReport.EmployeeRow> rows = new ArrayList<>();
        int rank = 1;
        for (Object[] row : raw) {
            long   txnCount   = ((Number) row[3]).longValue();
            BigDecimal revenue = new BigDecimal(row[4].toString()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal avgTicket = txnCount > 0
                    ? revenue.divide(BigDecimal.valueOf(txnCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            rows.add(EmployeePerformanceReport.EmployeeRow.builder()
                    .employeeId(((Number) row[0]).longValue())
                    .fullName(row[1] + " " + row[2])
                    .transactionCount(txnCount)
                    .totalRevenue(revenue)
                    .averageTicket(avgTicket)
                    .rank(rank++)
                    .build());
        }

        return EmployeePerformanceReport.builder()
                .from(from).to(to)
                .employees(rows)
                .build();
    }

    // ══════════════════════════════════════════════════════
    //  SHRINKAGE REPORT
    // ══════════════════════════════════════════════════════

    public ShrinkageReport shrinkage(Long tenantId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end   = to.atTime(LocalTime.MAX);

        List<AdjustmentType> shrinkageTypes = List.of(
                AdjustmentType.DAMAGE, AdjustmentType.THEFT,
                AdjustmentType.EXPIRY, AdjustmentType.MANUAL_REMOVE
        );

        List<Object[]> raw = adjustmentRepository.shrinkageByType(tenantId, shrinkageTypes, start, end);

        // Need product names — fetch by IDs
        Set<Long> productIds = raw.stream()
                .map(r -> ((Number) r[1]).longValue())
                .collect(Collectors.toSet());
        Map<Long, String> productNames = new HashMap<>();
        if (!productIds.isEmpty()) {
            productRepository.findAllById(productIds)
                    .forEach(p -> productNames.put(p.getProductId(), p.getName()));
        }

        List<ShrinkageReport.ShrinkageRow> items = raw.stream()
                .map(row -> ShrinkageReport.ShrinkageRow.builder()
                        .type((AdjustmentType) row[0])
                        .productId(((Number) row[1]).longValue())
                        .productName(productNames.getOrDefault(
                                ((Number) row[1]).longValue(), "Unknown"))
                        .unitsLost(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        long totalLost = items.stream().mapToLong(ShrinkageReport.ShrinkageRow::getUnitsLost).sum();

        Map<String, Long> byType = items.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getType().name(),
                        Collectors.summingLong(ShrinkageReport.ShrinkageRow::getUnitsLost)));

        return ShrinkageReport.builder()
                .from(from).to(to)
                .totalUnitsLost(totalLost)
                .byType(byType)
                .items(items)
                .build();
    }

    // ══════════════════════════════════════════════════════
    //  LOW STOCK REPORT
    // ══════════════════════════════════════════════════════

    public LowStockReport lowStock(Long tenantId) {
        List<Product> lowStockProducts = productRepository.findAll().stream()
                .filter(p -> p.getTenant().getTenantId().equals(tenantId))
                .filter(Product::getActive)
                .filter(Product::isLowStock)
                .sorted(Comparator.comparingInt(p ->
                        p.getStockQty() - (p.getReorderPoint() != null ? p.getReorderPoint() : 0)))
                .toList();

        List<LowStockReport.LowStockRow> rows = lowStockProducts.stream()
                .map(p -> {
                    int reorder  = p.getReorderPoint() != null ? p.getReorderPoint() : 5;
                    int suggested = Math.max(reorder * 2 - p.getStockQty(), reorder);
                    return LowStockReport.LowStockRow.builder()
                            .productId(p.getProductId())
                            .productName(p.getName())
                            .sku(p.getSku())
                            .category(p.getCategory())
                            .currentStock(p.getStockQty())
                            .reorderPoint(reorder)
                            .suggestedOrderQty(suggested)
                            .build();
                })
                .collect(Collectors.toList());

        return LowStockReport.builder()
                .totalProducts(rows.size())
                .products(rows)
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Long orZeroLong(Long value) {
        return value != null ? value : 0L;
    }
}
