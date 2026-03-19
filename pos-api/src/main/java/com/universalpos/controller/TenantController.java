package com.universalpos.controller;

import com.universalpos.domain.Tenant;
import com.universalpos.dto.response.ApiResponse;
import com.universalpos.exception.BusinessException;
import com.universalpos.exception.ResourceNotFoundException;
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

import java.util.Optional;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Company/store configuration (Admin only)")
public class TenantController {

    private final TenantRepository tenantRepository;

    @GetMapping("/current")
    @Operation(summary = "Get your current tenant configuration")
    public ResponseEntity<ApiResponse<Tenant>> getCurrent(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal) {
        Tenant tenant = tenantRepository.findById(principal.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", principal.getTenantId()));
        return ResponseEntity.ok(ApiResponse.ok(tenant));
    }

    @PutMapping("/current")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update your tenant configuration (Admin only)")
    public ResponseEntity<ApiResponse<Tenant>> update(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestBody Tenant updates) {

        Tenant tenant = tenantRepository.findById(principal.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", principal.getTenantId()));

        Optional.ofNullable(updates.getCompanyName()).ifPresent(tenant::setCompanyName);
        Optional.ofNullable(updates.getLogoUrl()).ifPresent(tenant::setLogoUrl);
        Optional.ofNullable(updates.getBrandColor()).ifPresent(tenant::setBrandColor);
        Optional.ofNullable(updates.getReceiptHeader()).ifPresent(tenant::setReceiptHeader);
        Optional.ofNullable(updates.getReceiptFooter()).ifPresent(tenant::setReceiptFooter);
        Optional.ofNullable(updates.getTaxRate()).ifPresent(tenant::setTaxRate);
        Optional.ofNullable(updates.getCurrencyCode()).ifPresent(tenant::setCurrencyCode);
        Optional.ofNullable(updates.getTimezone()).ifPresent(tenant::setTimezone);

        return ResponseEntity.ok(ApiResponse.ok("Tenant updated", tenantRepository.save(tenant)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new tenant/company (Admin only)")
    public ResponseEntity<ApiResponse<Tenant>> create(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestBody Tenant tenant) {

        if (tenantRepository.existsByTenantSlug(tenant.getTenantSlug())) {
            throw new BusinessException("Tenant slug '" + tenant.getTenantSlug() + "' is already taken.");
        }
        Tenant saved = tenantRepository.save(tenant);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tenant created", saved));
    }
}
