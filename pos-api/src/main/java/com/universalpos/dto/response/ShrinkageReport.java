package com.universalpos.dto.response;

import com.universalpos.domain.InventoryAdjustment.AdjustmentType;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Inventory shrinkage — units lost to damage, theft, expiry, etc.
 * Returned by GET /reports/shrinkage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShrinkageReport {

    private LocalDate from;
    private LocalDate to;
    private Long totalUnitsLost;
    private Map<String, Long> byType;       // e.g. {"DAMAGE": 5, "THEFT": 2}
    private List<ShrinkageRow> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShrinkageRow {
        private AdjustmentType type;
        private Long productId;
        private String productName;
        private Long unitsLost;
    }
}
