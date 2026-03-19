package com.universalpos.controller;

import com.universalpos.domain.Discount;
import com.universalpos.domain.Tenant;
import com.universalpos.dto.response.ApiResponse;
import com.universalpos.exception.ResourceNotFoundException;
import com.universalpos.repository.DiscountRepository;
import com.universalpos.repository.TenantRepository;
import com.universalpos.security.PosUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/discounts")
@RequiredArgsConstructor
@Tag(name = "Discounts", description = "Discount rule management")
public class DiscountController {

    private final DiscountRepository discountRepository;
    private final TenantRepository   tenantRepository;

    @GetMapping
    @Operation(summary = "List all discount rules for this tenant")
    public ResponseEntity<ApiResponse<List<Discount>>> list(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal) {
        List<Discount> discounts = discountRepository.findAll().stream()
                .filter(d -> d.getTenant().getTenantId().equals(principal.getTenantId()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(discounts));
    }

    @GetMapping("/active")
    @Operation(summary = "List currently valid/active discount rules")
    public ResponseEntity<ApiResponse<List<Discount>>> listActive(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal) {
        List<Discount> discounts = discountRepository
                .findValidDiscounts(principal.getTenantId(), LocalDate.now());
        return ResponseEntity.ok(ApiResponse.ok(discounts));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a discount rule by ID")
    public ResponseEntity<ApiResponse<Discount>> getById(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id) {
        Discount discount = discountRepository.findById(id)
                .filter(d -> d.getTenant().getTenantId().equals(principal.getTenantId()))
                .orElseThrow(() -> new ResourceNotFoundException("Discount", id));
        return ResponseEntity.ok(ApiResponse.ok(discount));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Create a discount rule (Manager+ only)")
    public ResponseEntity<ApiResponse<Discount>> create(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestBody Discount discount) {

        Tenant tenant = tenantRepository.findById(principal.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", principal.getTenantId()));
        discount.setTenant(tenant);
        discount.setTimesUsed(0);
        Discount saved = discountRepository.save(discount);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Discount rule created", saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Update a discount rule (Manager+ only)")
    public ResponseEntity<ApiResponse<Discount>> update(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Discount updates) {

        Discount existing = discountRepository.findById(id)
                .filter(d -> d.getTenant().getTenantId().equals(principal.getTenantId()))
                .orElseThrow(() -> new ResourceNotFoundException("Discount", id));

        Optional.ofNullable(updates.getName()).ifPresent(existing::setName);
        Optional.ofNullable(updates.getDescription()).ifPresent(existing::setDescription);
        Optional.ofNullable(updates.getValue()).ifPresent(existing::setValue);
        Optional.ofNullable(updates.getMinPurchase()).ifPresent(existing::setMinPurchase);
        Optional.ofNullable(updates.getStartDate()).ifPresent(existing::setStartDate);
        Optional.ofNullable(updates.getEndDate()).ifPresent(existing::setEndDate);
        Optional.ofNullable(updates.getActive()).ifPresent(existing::setActive);
        Optional.ofNullable(updates.getMaxUses()).ifPresent(existing::setMaxUses);

        return ResponseEntity.ok(ApiResponse.ok("Discount updated", discountRepository.save(existing)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Deactivate a discount rule (Manager+ only)")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id) {

        Discount discount = discountRepository.findById(id)
                .filter(d -> d.getTenant().getTenantId().equals(principal.getTenantId()))
                .orElseThrow(() -> new ResourceNotFoundException("Discount", id));
        discount.setActive(false);
        discountRepository.save(discount);
        return ResponseEntity.ok(ApiResponse.ok("Discount deactivated", null));
    }
}
