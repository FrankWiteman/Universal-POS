package com.universalpos.controller;

import com.universalpos.domain.ReturnReason;
import com.universalpos.dto.request.CreateReturnRequest;
import com.universalpos.dto.response.ApiResponse;
import com.universalpos.dto.response.TransactionResponse;
import com.universalpos.security.PosUserPrincipal;
import com.universalpos.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/returns")
@RequiredArgsConstructor
@Tag(name = "Returns & Exchanges", description = "Customer returns and product exchanges")
public class ReturnController {

    private final ReturnService returnService;

    // ── Return Reasons ────────────────────────────────────────

    @GetMapping("/reasons")
    @Operation(summary = "List all active return reason codes")
    public ResponseEntity<ApiResponse<List<ReturnReason>>> listReasons(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                returnService.getReturnReasons(principal.getTenantId())));
    }

    @PostMapping("/reasons")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Create a return reason code (Manager+ only)")
    public ResponseEntity<ApiResponse<ReturnReason>> createReason(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestBody ReturnReason reason) {
        ReturnReason saved = returnService.createReturnReason(reason, principal.getTenantId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Return reason created", saved));
    }

    // ── Returns ───────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Process a return — refund items from a completed sale")
    public ResponseEntity<ApiResponse<TransactionResponse>> processReturn(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @Valid @RequestBody CreateReturnRequest request) {

        TransactionResponse response = returnService.processReturn(
                request,
                principal.getEmployeeId(),
                principal.getTenantId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Return processed successfully", response));
    }

    // ── Exchanges ─────────────────────────────────────────────

    @PostMapping("/exchange")
    @Operation(summary = "Process an exchange — return items and take new ones")
    public ResponseEntity<ApiResponse<TransactionResponse>> processExchange(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @Valid @RequestBody CreateReturnRequest request) {

        TransactionResponse response = returnService.processExchange(
                request,
                principal.getEmployeeId(),
                principal.getTenantId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Exchange processed successfully", response));
    }
}
