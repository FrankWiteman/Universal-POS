package com.universalpos.controller;

import com.universalpos.dto.response.*;
import com.universalpos.security.PosUserPrincipal;
import com.universalpos.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@Tag(name = "Reports", description = "Manager reporting dashboard (Manager+ only)")
public class ReportController {

    private final ReportingService reportingService;

    @GetMapping("/daily")
    @Operation(summary = "Daily sales summary — revenue, transactions, returns, hourly breakdown")
    public ResponseEntity<ApiResponse<DailySalesReport>> dailySales(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate effectiveFrom = from != null ? from : LocalDate.now();
        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();

        return ResponseEntity.ok(ApiResponse.ok(
                reportingService.dailySales(principal.getTenantId(), effectiveFrom, effectiveTo)));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Top selling products by revenue or units sold")
    public ResponseEntity<ApiResponse<TopProductsReport>> topProducts(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "REVENUE") String sortBy,
            @RequestParam(defaultValue = "10") int limit) {

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();

        return ResponseEntity.ok(ApiResponse.ok(
                reportingService.topProducts(principal.getTenantId(),
                        effectiveFrom, effectiveTo, sortBy, limit)));
    }

    @GetMapping("/employee-performance")
    @Operation(summary = "Sales performance per employee — transactions and revenue")
    public ResponseEntity<ApiResponse<EmployeePerformanceReport>> employeePerformance(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();

        return ResponseEntity.ok(ApiResponse.ok(
                reportingService.employeePerformance(principal.getTenantId(),
                        effectiveFrom, effectiveTo)));
    }

    @GetMapping("/shrinkage")
    @Operation(summary = "Inventory shrinkage — units lost to damage, theft, expiry")
    public ResponseEntity<ApiResponse<ShrinkageReport>> shrinkage(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();

        return ResponseEntity.ok(ApiResponse.ok(
                reportingService.shrinkage(principal.getTenantId(),
                        effectiveFrom, effectiveTo)));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Products at or below reorder point with suggested order quantities")
    public ResponseEntity<ApiResponse<LowStockReport>> lowStock(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.ok(
                reportingService.lowStock(principal.getTenantId())));
    }
}
