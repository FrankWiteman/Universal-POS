package com.universalpos.controller;

import com.universalpos.domain.Product;
import com.universalpos.dto.response.ApiResponse;
import com.universalpos.security.PosUserPrincipal;
import com.universalpos.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog and inventory")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/search")
    @Operation(summary = "Search products by name, SKU, barcode, or brand")
    public ResponseEntity<ApiResponse<Page<Product>>> search(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                productService.search(principal.getTenantId(), q, PageRequest.of(page, size))));
    }

    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Look up a product by barcode (scanner input)")
    public ResponseEntity<ApiResponse<Product>> getByBarcode(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable String barcode) {
        return ResponseEntity.ok(ApiResponse.ok(
                productService.getByBarcode(barcode, principal.getTenantId())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<Product>> getById(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                productService.getById(id, principal.getTenantId())));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Create a new product (Manager+ only)")
    public ResponseEntity<ApiResponse<Product>> create(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestBody Product product) {
        Product saved = productService.create(product, principal.getTenantId(),
                principal.getEmployeeId(), principal.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product created", saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Update a product (Manager+ only)")
    public ResponseEntity<ApiResponse<Product>> update(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Product updates) {
        Product saved = productService.update(id, updates, principal.getTenantId(),
                principal.getEmployeeId(), principal.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Product updated", saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Deactivate a product (Manager+ only)")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id) {
        productService.deactivate(id, principal.getTenantId(),
                principal.getEmployeeId(), principal.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Product deactivated", null));
    }
}
