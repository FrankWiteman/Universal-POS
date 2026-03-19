package com.universalpos.dto.response;

import lombok.*;
import java.util.List;

/**
 * Products at or below reorder point with suggested PO quantities.
 * Returned by GET /reports/low-stock
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockReport {

    private int totalProducts;
    private List<LowStockRow> products;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockRow {
        private Long productId;
        private String productName;
        private String sku;
        private String category;
        private Integer currentStock;
        private Integer reorderPoint;
        private Integer suggestedOrderQty;   // reorderPoint * 2 - currentStock
        private String preferredSupplier;    // from ProductSuppliers if linked
    }
}
