package com.universalpos.service;

import com.universalpos.domain.*;
import com.universalpos.dto.request.CreateTransactionRequest;
import com.universalpos.dto.response.TransactionResponse;
import com.universalpos.engine.DiscountEngine;
import com.universalpos.exception.BusinessException;
import com.universalpos.exception.ResourceNotFoundException;
import com.universalpos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProductRepository     productRepository;
    private final CustomerRepository    customerRepository;
    private final EmployeeRepository    employeeRepository;
    private final TenantRepository      tenantRepository;
    private final DiscountEngine        discountEngine;
    private final CustomerService       customerService;
    private final AuditService          auditService;

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long txnId, Long tenantId) {
        Transaction t = transactionRepository.findById(txnId)
                .filter(tx -> tx.getTenant().getTenantId().equals(tenantId))
                .orElseThrow(() -> new com.universalpos.exception.ResourceNotFoundException("Transaction", txnId));
        return toResponse(t);
    }

    /**
     * Process and complete a sale transaction.
     *
     * Flow:
     *   1. Validate all products exist and are in stock
     *   2. Build cart map (Product → qty)
     *   3. Calculate subtotal
     *   4. Run discount engine → get best discount
     *   5. Calculate tax
     *   6. Persist transaction + items
     *   7. Decrement inventory
     *   8. Award loyalty points
     *   9. Generate receipt
     *  10. Audit log
     */
    @Transactional
    public TransactionResponse createSale(CreateTransactionRequest request,
                                          Long employeeId, Long tenantId) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        Employee employee = employeeRepository
                .findByEmployeeIdAndTenant_TenantId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .filter(c -> c.getTenant().getTenantId().equals(tenantId))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Customer", request.getCustomerId()));
        }

        // ── Step 1: Resolve and validate cart items ──────────────
        Map<Product, Integer> cartMap = new LinkedHashMap<>();
        for (CreateTransactionRequest.TransactionItemRequest itemReq : request.getItems()) {
            Product product = productRepository
                    .findById(itemReq.getProductId())
                    .filter(p -> p.getTenant().getTenantId().equals(tenantId))
                    .filter(Product::getActive)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product", itemReq.getProductId()));

            if (product.getStockQty() < itemReq.getQty()) {
                throw new BusinessException(
                        "Insufficient stock for product: " + product.getName() +
                        " (available: " + product.getStockQty() +
                        ", requested: " + itemReq.getQty() + ")");
            }
            cartMap.put(product, itemReq.getQty());
        }

        // ── Step 2: Calculate subtotal ───────────────────────────
        BigDecimal subtotal = cartMap.entrySet().stream()
                .map(e -> e.getKey().getPrice()
                           .multiply(BigDecimal.valueOf(e.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // ── Step 3: Run discount engine ──────────────────────────
        DiscountEngine.DiscountResult discountResult = discountEngine.evaluate(
                tenantId, customer, cartMap, subtotal, request.getCouponCode());

        BigDecimal discountAmount = discountResult.discountAmount();
        BigDecimal discountedSubtotal = subtotal.subtract(discountAmount)
                .max(BigDecimal.ZERO);

        // ── Step 4: Calculate tax ────────────────────────────────
        // Only taxable items contribute to tax
        BigDecimal taxableAmount = cartMap.entrySet().stream()
                .filter(e -> e.getKey().getTaxable())
                .map(e -> e.getKey().getPrice()
                           .multiply(BigDecimal.valueOf(e.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Apply the discount proportionally to taxable amount
        BigDecimal taxableAfterDiscount = taxableAmount.subtract(
                discountAmount.multiply(
                        taxableAmount.divide(subtotal, 10, RoundingMode.HALF_UP))
        ).max(BigDecimal.ZERO);

        BigDecimal taxRate   = BigDecimal.valueOf(tenant.getTaxRate());
        BigDecimal taxAmount = taxableAfterDiscount.multiply(taxRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = discountedSubtotal.add(taxAmount)
                .setScale(2, RoundingMode.HALF_UP);

        // ── Step 5: Calculate change for cash ───────────────────
        BigDecimal changeDue = BigDecimal.ZERO;
        if (request.getPaymentMethod() == Transaction.PaymentMethod.CASH
                && request.getAmountTendered() != null) {
            if (request.getAmountTendered().compareTo(total) < 0) {
                throw new BusinessException("Amount tendered ($" +
                        request.getAmountTendered() + ") is less than total ($" + total + ")");
            }
            changeDue = request.getAmountTendered().subtract(total)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // ── Step 6: Loyalty points earned (1 pt per dollar) ─────
        int pointsEarned = total.intValue();

        // ── Step 7: Build and persist transaction ────────────────
        Transaction transaction = Transaction.builder()
                .tenant(tenant)
                .customer(customer)
                .employee(employee)
                .receiptNumber(generateReceiptNumber())
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .taxAmount(taxAmount)
                .total(total)
                .paymentMethod(request.getPaymentMethod())
                .amountTendered(request.getAmountTendered())
                .changeDue(changeDue)
                .status(Transaction.TransactionStatus.COMPLETED)
                .txnType(Transaction.TransactionType.SALE)
                .loyaltyPointsEarned(pointsEarned)
                .notes(request.getNotes())
                .completedAt(LocalDateTime.now())
                .build();

        // Build line items
        List<TransactionItem> items = new ArrayList<>();
        for (Map.Entry<Product, Integer> entry : cartMap.entrySet()) {
            Product product = entry.getKey();
            int qty         = entry.getValue();

            // Pro-rate the discount across items by their share of subtotal
            BigDecimal itemSubtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(qty));
            BigDecimal itemDiscount = subtotal.compareTo(BigDecimal.ZERO) > 0
                    ? discountAmount.multiply(
                            itemSubtotal.divide(subtotal, 10, RoundingMode.HALF_UP))
                      .setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal lineTotal = itemSubtotal.subtract(itemDiscount)
                    .setScale(2, RoundingMode.HALF_UP);

            items.add(TransactionItem.builder()
                    .transaction(transaction)
                    .product(product)
                    .qty(qty)
                    .unitPrice(product.getPrice())
                    .discountApplied(itemDiscount)
                    .discountLabel(discountResult.discountLabel())
                    .lineTotal(lineTotal)
                    .build());
        }
        transaction.setItems(items);
        transaction = transactionRepository.save(transaction);

        // ── Step 8: Decrement inventory ──────────────────────────
        for (Map.Entry<Product, Integer> entry : cartMap.entrySet()) {
            Product product = entry.getKey();
            product.setStockQty(product.getStockQty() - entry.getValue());
            productRepository.save(product);
        }

        // ── Step 9: Award loyalty points ─────────────────────────
        if (customer != null && pointsEarned > 0) {
            customerService.addLoyaltyPoints(customer.getCustomerId(), tenantId, pointsEarned);
        }

        // ── Step 10: Audit ───────────────────────────────────────
        auditService.log(tenantId, employeeId, employee.getFullName(),
                "SALE", "TRANSACTION", transaction.getTxnId(),
                String.format("Sale completed: %s | total=$%.2f | discount=$%.2f",
                              transaction.getReceiptNumber(), total, discountAmount));

        log.info("Transaction {} completed: total=${} discount=${} tax=${}",
                 transaction.getReceiptNumber(), total, discountAmount, taxAmount);

        return toResponse(transaction);
    }

    /**
     * Void a completed transaction (Manager+ only).
     * Creates a compensating VOID transaction rather than deleting.
     */
    @Transactional
    public TransactionResponse voidTransaction(Long txnId, Long employeeId,
                                                Long tenantId, String reason) {
        Transaction original = transactionRepository.findById(txnId)
                .filter(t -> t.getTenant().getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", txnId));

        if (original.getStatus() != Transaction.TransactionStatus.COMPLETED) {
            throw new BusinessException("Only COMPLETED transactions can be voided. " +
                    "Current status: " + original.getStatus());
        }

        Employee employee = employeeRepository
                .findByEmployeeIdAndTenant_TenantId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        // Mark original as voided
        original.setStatus(Transaction.TransactionStatus.VOIDED);
        transactionRepository.save(original);

        // Restore inventory
        for (TransactionItem item : original.getItems()) {
            Product product = item.getProduct();
            product.setStockQty(product.getStockQty() + item.getQty());
            productRepository.save(product);
        }

        // Reverse loyalty points
        if (original.getCustomer() != null && original.getLoyaltyPointsEarned() > 0) {
            // Negative points to subtract
            customerService.addLoyaltyPoints(
                    original.getCustomer().getCustomerId(), tenantId,
                    -original.getLoyaltyPointsEarned());
        }

        auditService.log(tenantId, employeeId, employee.getFullName(),
                "VOID", "TRANSACTION", txnId,
                "Void reason: " + reason);

        log.info("Transaction {} voided by employee {}",
                 original.getReceiptNumber(), employee.getFullName());

        return toResponse(original);
    }

    // ── Private helpers ──────────────────────────────────────────

    private String generateReceiptNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rand = String.format("%04d", new Random().nextInt(10000));
        return "TXN-" + date + "-" + rand;
    }

    private TransactionResponse toResponse(Transaction t) {
        List<TransactionResponse.LineItemResponse> lineItems = t.getItems().stream()
                .map(item -> TransactionResponse.LineItemResponse.builder()
                        .productId(item.getProduct().getProductId())
                        .productName(item.getProduct().getName())
                        .sku(item.getProduct().getSku())
                        .qty(item.getQty())
                        .unitPrice(item.getUnitPrice())
                        .discountApplied(item.getDiscountApplied())
                        .discountLabel(item.getDiscountLabel())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        return TransactionResponse.builder()
                .txnId(t.getTxnId())
                .receiptNumber(t.getReceiptNumber())
                .employeeName(t.getEmployee().getFullName())
                .items(lineItems)
                .subtotal(t.getSubtotal())
                .discountAmount(t.getDiscountAmount())
                .taxAmount(t.getTaxAmount())
                .total(t.getTotal())
                .amountTendered(t.getAmountTendered())
                .changeDue(t.getChangeDue())
                .paymentMethod(t.getPaymentMethod())
                .status(t.getStatus())
                .txnType(t.getTxnType())
                .loyaltyPointsEarned(t.getLoyaltyPointsEarned())
                .completedAt(t.getCompletedAt())
                .build();
    }
}
