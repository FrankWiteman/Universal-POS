package com.universalpos.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Sales performance per employee for a date range.
 * Returned by GET /reports/employee-performance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePerformanceReport {

    private LocalDate from;
    private LocalDate to;
    private List<EmployeeRow> employees;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeRow {
        private Long employeeId;
        private String fullName;
        private Long transactionCount;
        private BigDecimal totalRevenue;
        private BigDecimal averageTicket;
        private int rank;
    }
}
