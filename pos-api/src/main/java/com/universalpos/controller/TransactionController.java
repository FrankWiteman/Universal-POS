package com.universalpos.controller;

import com.universalpos.dto.request.CreateTransactionRequest;
import com.universalpos.dto.response.ApiResponse;
import com.universalpos.dto.response.TransactionResponse;
import com.universalpos.security.PosUserPrincipal;
import com.universalpos.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "POS sales, returns, and voids")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Process a sale transaction")
    public ResponseEntity<ApiResponse<TransactionResponse>> createSale(
            @AuthenticationPrincipal PosUserPrincipal principal,
            @Valid @RequestBody CreateTransactionRequest request) {

        TransactionResponse txn = transactionService.createSale(
                request, principal.getEmployeeId(), principal.getTenantId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transaction completed", txn));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Void a completed transaction (Manager+ only)")
    public ResponseEntity<ApiResponse<TransactionResponse>> voidTransaction(
            @AuthenticationPrincipal PosUserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String reason = body.getOrDefault("reason", "No reason provided");
        TransactionResponse txn = transactionService.voidTransaction(
                id, principal.getEmployeeId(), principal.getTenantId(), reason);
        return ResponseEntity.ok(ApiResponse.ok("Transaction voided", txn));
    }
}
