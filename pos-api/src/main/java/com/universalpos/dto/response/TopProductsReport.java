package com.universalpos.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Best-performing products by revenue or units sold.
 * Returned by GET /reports/top-products
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductsReport {

    private LocalDate from;
    private LocalDate to;
    private String sortedBy;              // "REVENUE" or "UNITS"
    private List<ProductRow> products;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductRow {
        private Long productId;
        private String productName;
        private String sku;
        private Long unitsSold;
        private BigDecimal revenue;
        private int rank;
    }
}
