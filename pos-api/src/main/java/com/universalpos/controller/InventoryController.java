package com.universalpos.controller;

import com.universalpos.domain.*;
import com.universalpos.dto.response.ApiResponse;
import com.universalpos.security.PosUserPrincipal;
import com.universalpos.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Suppliers, purchase orders, stock counts, and adjustments")
public class InventoryController {

    private final InventoryService inventoryService;

    // ── Low Stock ─────────────────────────────────────────────────

    @GetMapping("/low-stock")
    @Operation(summary = "Get all products at or below their reorder point")
    public ResponseEntity<ApiResponse<List<Product>>> getLowStock(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal) {
        List<Product> products = inventoryService.getLowStockProducts(principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    // ── Suppliers ─────────────────────────────────────────────────

    @GetMapping("/suppliers")
    @Operation(summary = "List all active suppliers")
    public ResponseEntity<ApiResponse<List<Supplier>>> listSuppliers(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal) {
        List<Supplier> suppliers = inventoryService.getAllSuppliers(principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.ok(suppliers));
    }

    @GetMapping("/suppliers/search")
    @Operation(summary = "Search suppliers by name or contact")
    public ResponseEntity<ApiResponse<Page<Supplier>>> searchSuppliers(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Supplier> results = inventoryService.searchSuppliers(
                principal.getTenantId(), q, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @PostMapping("/suppliers")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Create a new supplier (Manager+ only)")
    public ResponseEntity<ApiResponse<Supplier>> createSupplier(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestBody Supplier supplier) {
        Supplier saved = inventoryService.createSupplier(
                supplier, principal.getTenantId(),
                principal.getEmployeeId(), principal.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Supplier created", saved));
    }

    @PutMapping("/suppliers/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Update a supplier (Manager+ only)")
    public ResponseEntity<ApiResponse<Supplier>> updateSupplier(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Supplier updates) {
        Supplier updated = inventoryService.updateSupplier(
                id, updates, principal.getTenantId(),
                principal.getEmployeeId(), principal.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Supplier updated", updated));
    }

    // ── Purchase Orders ───────────────────────────────────────────

    @GetMapping("/purchase-orders")
    @Operation(summary = "List all purchase orders")
    public ResponseEntity<ApiResponse<Page<PurchaseOrder>>> listPurchaseOrders(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PurchaseOrder> orders = inventoryService.listPurchaseOrders(
                principal.getTenantId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/purchase-orders/{id}")
    @Operation(summary = "Get a purchase order by ID")
    public ResponseEntity<ApiResponse<PurchaseOrder>> getPurchaseOrder(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id) {
        PurchaseOrder po = inventoryService.getPurchaseOrder(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.ok(po));
    }

    @PostMapping("/purchase-orders")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Create a new purchase order (Manager+ only)")
    public ResponseEntity<ApiResponse<PurchaseOrder>> createPurchaseOrder(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestBody Map<String, Object> body) {

        Long supplierId    = Long.valueOf(body.get("supplierId").toString());
        LocalDate expected = body.containsKey("expectedDate")
                ? LocalDate.parse(body.get("expectedDate").toString()) : null;
        String notes       = body.containsKey("notes") ? body.get("notes").toString() : null;

        PurchaseOrder po = inventoryService.createPurchaseOrder(
                supplierId, expected, notes,
                principal.getTenantId(),
                principal.getEmployeeId(), principal.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Purchase order created", po));
    }

    @PostMapping("/purchase-orders/{id}/items")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Add an item to a draft purchase order (Manager+ only)")
    public ResponseEntity<ApiResponse<PurchaseOrder>> addItem(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Long productId   = Long.valueOf(body.get("productId").toString());
        Integer qty      = Integer.valueOf(body.get("qty").toString());
        BigDecimal cost  = new BigDecimal(body.get("unitCost").toString());

        PurchaseOrder po = inventoryService.addItemToPurchaseOrder(
                id, productId, qty, cost, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.ok("Item added to purchase order", po));
    }

    @PostMapping("/purchase-orders/{id}/submit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Submit a purchase order to the supplier (Manager+ only)")
    public ResponseEntity<ApiResponse<PurchaseOrder>> submitPurchaseOrder(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id) {
        PurchaseOrder po = inventoryService.submitPurchaseOrder(
                id, principal.getTenantId(),
                principal.getEmployeeId(), principal.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Purchase order submitted", po));
    }

    @PostMapping("/purchase-orders/{id}/receive")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Record receipt of items from a purchase order (Manager+ only)")
    public ResponseEntity<ApiResponse<PurchaseOrder>> receivePurchaseOrder(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, Integer> receivedQuantities) {
        // body: { "itemId1": qty, "itemId2": qty, ... }
        Map<Long, Integer> longKeyMap = new java.util.HashMap<>();
        receivedQuantities.forEach((k, v) -> longKeyMap.put(Long.valueOf(k), v));

        PurchaseOrder po = inventoryService.receivePurchaseOrder(
                id, longKeyMap, principal.getTenantId(),
                principal.getEmployeeId(), principal.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Items received — stock updated", po));
    }

    // ── Manual Adjustments ────────────────────────────────────────

    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Record a manual stock adjustment (damage, theft, correction) — Manager+ only")
    public ResponseEntity<ApiResponse<InventoryAdjustment>> adjust(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestBody Map<String, Object> body) {

        Long productId = Long.valueOf(body.get("productId").toString());
        InventoryAdjustment.AdjustmentType type =
                InventoryAdjustment.AdjustmentType.valueOf(body.get("type").toString());
        Integer qtyChange = Integer.valueOf(body.get("qtyChange").toString());
        String reason     = body.containsKey("reason") ? body.get("reason").toString() : null;

        InventoryAdjustment adj = inventoryService.adjustStock(
                productId, type, qtyChange, reason,
                principal.getTenantId(),
                principal.getEmployeeId(), principal.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Stock adjustment recorded", adj));
    }

    // ── Stock Counts ──────────────────────────────────────────────

    @PostMapping("/stock-counts/start")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Start a new stock count session (Manager+ only)")
    public ResponseEntity<ApiResponse<StockCount>> startStockCount(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestBody Map<String, String> body) {

        StockCount.CountType type = StockCount.CountType.valueOf(
                body.getOrDefault("countType", "FULL"));
        String category = body.getOrDefault("categoryFilter", null);

        StockCount count = inventoryService.startStockCount(
                type, category, principal.getTenantId(),
                principal.getEmployeeId(), principal.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Stock count started — " + count.getItems().size() + " products to count", count));
    }

    @PostMapping("/stock-counts/{id}/complete")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Complete a stock count and apply variance corrections (Manager+ only)")
    public ResponseEntity<ApiResponse<StockCount>> completeStockCount(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id) {
        StockCount count = inventoryService.completeStockCount(
                id, principal.getTenantId(),
                principal.getEmployeeId(), principal.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Stock count completed — variances applied", count));
    }
}
