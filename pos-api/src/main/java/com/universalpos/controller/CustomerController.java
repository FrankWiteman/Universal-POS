package com.universalpos.controller;

import com.universalpos.dto.request.CreateCustomerRequest;
import com.universalpos.dto.response.ApiResponse;
import com.universalpos.dto.response.CustomerResponse;
import com.universalpos.security.PosUserPrincipal;
import com.universalpos.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer lookup and management")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/search")
    @Operation(summary = "Search customers by phone, email, name, or loyalty card")
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> search(
            @AuthenticationPrincipal PosUserPrincipal principal,
            @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<CustomerResponse> results = customerService.search(
                principal.getTenantId(), q, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(
            @AuthenticationPrincipal PosUserPrincipal principal,
            @PathVariable Long id) {
        CustomerResponse customer = customerService.getById(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.ok(customer));
    }

    @PostMapping
    @Operation(summary = "Create a new customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(
            @AuthenticationPrincipal PosUserPrincipal principal,
            @Valid @RequestBody CreateCustomerRequest request) {

        CustomerResponse customer = customerService.create(
                request, principal.getTenantId(),
                principal.getEmployeeId(), principal.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Customer created successfully", customer));
    }
}
