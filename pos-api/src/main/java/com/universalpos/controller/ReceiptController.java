package com.universalpos.controller;

import com.universalpos.dto.response.ApiResponse;
import com.universalpos.security.PosUserPrincipal;
import com.universalpos.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipts", description = "Email and PDF receipt generation")
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping("/{txnId}/email")
    @Operation(summary = "Email a receipt to an address")
    public ResponseEntity<ApiResponse<Void>> emailReceipt(
            @AuthenticationPrincipal PosUserPrincipal principal,
            @PathVariable Long txnId,
            @RequestBody Map<String, String> body) {

        String emailAddress = body.get("email");
        receiptService.emailReceipt(txnId, principal.getTenantId(), emailAddress);
        return ResponseEntity.ok(ApiResponse.ok("Receipt sent to " + emailAddress, null));
    }

    @GetMapping("/{txnId}/pdf")
    @Operation(summary = "Download a receipt as PDF")
    public ResponseEntity<byte[]> downloadPdf(
            @AuthenticationPrincipal PosUserPrincipal principal,
            @PathVariable Long txnId) {

        byte[] pdf = receiptService.generatePdf(txnId, principal.getTenantId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"receipt-" + txnId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF != null ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM)
                .body(pdf);
    }
}
