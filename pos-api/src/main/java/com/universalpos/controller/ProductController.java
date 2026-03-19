package com.universalpos.controller;

import com.universalpos.domain.Product;
import com.universalpos.dto.response.ApiResponse;
import com.universalpos.exception.ResourceNotFoundException;
import com.universalpos.repository.ProductRepository;
import com.universalpos.repository.TenantRepository;
import com.universalpos.security.PosUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog and inventory")
public class ProductController {

    private final ProductRepository productRepository;
    private final TenantRepository  tenantRepository;

    @GetMapping("/search")
    @Operation(summary = "Search products by name, SKU, barcode, or brand")
    public ResponseEntity<ApiResponse<Page<Product>>> search(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Product> results = productRepository.searchProducts(
                principal.getTenantId(), q, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Look up a product by barcode (scanner input)")
    public ResponseEntity<ApiResponse<Product>> getByBarcode(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable String barcode) {

        Product product = productRepository
                .findByBarcodeAndTenant_TenantIdAndActiveTrue(barcode, principal.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "barcode", barcode));
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<Product>> getById(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id) {

        Product product = productRepository.findById(id)
                .filter(p -> p.getTenant().getTenantId().equals(principal.getTenantId()))
                .filter(Product::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Create a new product (Manager+ only)")
    public ResponseEntity<ApiResponse<Product>> create(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestBody Product productRequest) {

        var tenant = tenantRepository.findById(principal.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", principal.getTenantId()));

        productRequest.setTenant(tenant);
        productRequest.setActive(true);
        Product saved = productRepository.save(productRequest);
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

        Product existing = productRepository.findById(id)
                .filter(p -> p.getTenant().getTenantId().equals(principal.getTenantId()))
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        Optional.ofNullable(updates.getName()).ifPresent(existing::setName);
        Optional.ofNullable(updates.getDescription()).ifPresent(existing::setDescription);
        Optional.ofNullable(updates.getPrice()).ifPresent(existing::setPrice);
        Optional.ofNullable(updates.getStockQty()).ifPresent(existing::setStockQty);
        Optional.ofNullable(updates.getCategory()).ifPresent(existing::setCategory);
        Optional.ofNullable(updates.getBarcode()).ifPresent(existing::setBarcode);
        Optional.ofNullable(updates.getActive()).ifPresent(existing::setActive);

        Product saved = productRepository.save(existing);
        return ResponseEntity.ok(ApiResponse.ok("Product updated", saved));
    }
}
