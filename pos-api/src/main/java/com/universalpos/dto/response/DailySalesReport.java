package com.universalpos.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Summary of sales activity for a date range.
 * Returned by GET /reports/daily
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesReport {

    private LocalDate from;
    private LocalDate to;

    // ── Sales ─────────────────────────────────────────────────
    private Long totalTransactions;
    private BigDecimal grossRevenue;        // before discounts
    private BigDecimal totalDiscounts;      // total discount amount given
    private BigDecimal totalTax;            // tax collected
    private BigDecimal netRevenue;          // gross - discounts (pre-tax)
    private BigDecimal averageTicket;       // grossRevenue / totalTransactions

    // ── Returns ───────────────────────────────────────────────
    private Long totalReturns;
    private BigDecimal totalRefunded;       // total $ refunded
    private BigDecimal returnRate;          // returns / transactions as %

    // ── Hourly Breakdown ──────────────────────────────────────
    private List<HourlyBreakdown> byHour;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyBreakdown {
        private String hour;                // "09", "10", "13" etc.
        private String hourLabel;           // "9 AM", "1 PM" etc.
        private Long transactions;
        private BigDecimal revenue;
    }
}
