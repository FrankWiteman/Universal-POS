package com.universalpos.service;

import com.universalpos.domain.*;
import com.universalpos.domain.InventoryAdjustment.AdjustmentType;
import com.universalpos.domain.Transaction.*;
import com.universalpos.dto.request.CreateReturnRequest;
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

/**
 * ════════════════════════════════════════════════════════════
 *  ReturnService — Phase 2
 * ════════════════════════════════════════════════════════════
 *
 *  Handles two workflows:
 *
 *  1. RETURN — customer brings items back for a refund
 *     - Validate original transaction exists and is COMPLETED
 *     - Validate quantities (can't return more than was bought,
 *       or more than hasn't already been returned)
 *     - Create a RETURN transaction linked to the original
 *     - Restock items unless marked damaged
 *     - Reverse loyalty points proportionally
 *     - Update original transaction status to REFUNDED
 *     - Write inventory adjustment records
 *
 *  2. EXCHANGE — customer returns items and takes new ones
 *     - Process the return portion (same as above)
 *     - Process a new SALE for the exchange items
 *     - Calculate net amount: if new items cost more → charge difference
 *       if new items cost less → refund the difference
 *     - Both transactions are linked to the original
 *     - Single atomic DB commit for the entire exchange
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnService {

    private final TransactionRepository        transactionRepository;
    private final TransactionItemRepository    transactionItemRepository;
    private final ProductRepository            productRepository;
    private final EmployeeRepository           employeeRepository;
    private final ReturnItemRepository         returnItemRepository;
    private final ReturnReasonRepository       returnReasonRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final CustomerService              customerService;
    private final DiscountEngine               discountEngine;
    private final AuditService                 auditService;

    // ══════════════════════════════════════════════════════
    //  RETURN
    // ══════════════════════════════════════════════════════

    /**
     * Process a return against an original sale transaction.
     * Supports partial returns (return only some items or some qty).
     */
    @Transactional
    public TransactionResponse processReturn(CreateReturnRequest request,
                                              Long employeeId, Long tenantId) {

        // 1. Load and validate the original transaction
        Transaction original = loadAndValidateOriginal(request.getOriginalTxnId(), tenantId);

        Employee employee = employeeRepository
                .findByEmployeeIdAndTenant_TenantId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        // 2. Build return line items and calculate refund
        List<ReturnItem> returnItems = new ArrayList<>();
        BigDecimal totalRefund = BigDecimal.ZERO;

        for (CreateReturnRequest.ReturnLineRequest lineReq : request.getReturnItems()) {
            TransactionItem originalItem = findAndValidateItem(
                    lineReq.getOriginalItemId(), original, lineReq.getQtyToReturn());

            ReturnReason reason = lineReq.getReasonId() != null
                    ? returnReasonRepository.findById(lineReq.getReasonId()).orElse(null)
                    : null;

            // Refund = unit price × qty being returned
            BigDecimal lineRefund = originalItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(lineReq.getQtyToReturn()))
                    .setScale(2, RoundingMode.HALF_UP);

            ReturnItem returnItem = ReturnItem.builder()
                    .originalTxnId(original.getTxnId())
                    .originalItem(originalItem)
                    .product(originalItem.getProduct())
                    .qtyReturned(lineReq.getQtyToReturn())
                    .unitPrice(originalItem.getUnitPrice())
                    .refundAmount(lineRefund)
                    .reason(reason)
                    .restock(lineReq.getRestock())
                    .build();

            returnItems.add(returnItem);
            totalRefund = totalRefund.add(lineRefund);
        }

        // 3. Calculate tax refund proportional to items being returned
        BigDecimal returnSubtotal   = returnItems.stream()
                .map(ReturnItem::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxRefund = BigDecimal.ZERO;
        if (original.getSubtotal().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal taxRate = original.getTaxAmount()
                    .divide(original.getSubtotal(), 10, RoundingMode.HALF_UP);
            taxRefund = returnSubtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal totalWithTax = returnSubtotal.add(taxRefund);

        // 4. Create the RETURN transaction
        Transaction returnTxn = Transaction.builder()
                .tenant(original.getTenant())
                .customer(original.getCustomer())
                .employee(employee)
                .receiptNumber(generateReceiptNumber("RTN"))
                .subtotal(returnSubtotal.negate())
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(taxRefund.negate())
                .total(totalWithTax.negate())           // negative = money going OUT
                .paymentMethod(request.getRefundPaymentMethod())
                .status(TransactionStatus.COMPLETED)
                .txnType(TransactionType.RETURN)
                .originalTxnId(original.getTxnId())
                .loyaltyPointsEarned(0)
                .notes(request.getNotes())
                .completedAt(LocalDateTime.now())
                .build();

        returnTxn = transactionRepository.save(returnTxn);

        // 5. Save return items, restock inventory, write adjustment records
        for (ReturnItem ri : returnItems) {
            ri.setReturnTransaction(returnTxn);
            returnItemRepository.save(ri);

            if (Boolean.TRUE.equals(ri.getRestock())) {
                Product product = ri.getProduct();
                int qtyBefore = product.getStockQty();
                product.setStockQty(qtyBefore + ri.getQtyReturned());
                productRepository.save(product);

                adjustmentRepository.save(InventoryAdjustment.builder()
                        .tenantId(tenantId)
                        .productId(product.getProductId())
                        .employeeId(employeeId)
                        .adjustmentType(AdjustmentType.RETURN)
                        .qtyBefore(qtyBefore)
                        .qtyChange(ri.getQtyReturned())
                        .qtyAfter(product.getStockQty())
                        .reason("Return on " + returnTxn.getReceiptNumber())
                        .referenceId(returnTxn.getTxnId())
                        .build());
            }
        }

        // 6. Reverse loyalty points proportionally
        if (original.getCustomer() != null && original.getLoyaltyPointsEarned() > 0
                && original.getTotal().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = returnSubtotal.divide(original.getSubtotal(), 10, RoundingMode.HALF_UP);
            int pointsToReverse = ratio.multiply(BigDecimal.valueOf(original.getLoyaltyPointsEarned()))
                    .intValue();
            if (pointsToReverse > 0) {
                customerService.addLoyaltyPoints(
                        original.getCustomer().getCustomerId(), tenantId, -pointsToReverse);
            }
        }

        // 7. Update original transaction status
        boolean allItemsReturned = isFullyReturned(original);
        if (allItemsReturned) {
            original.setStatus(TransactionStatus.REFUNDED);
            transactionRepository.save(original);
        }

        auditService.log(tenantId, employeeId, employee.getFullName(),
                "RETURN", "TRANSACTION", returnTxn.getTxnId(),
                "Return for original TXN " + original.getReceiptNumber() +
                " | refund=" + totalWithTax);

        log.info("Return {} processed against {} | refund=${} | {} items",
                returnTxn.getReceiptNumber(), original.getReceiptNumber(),
                totalWithTax, returnItems.size());

        return toResponse(returnTxn, returnItems);
    }

    // ══════════════════════════════════════════════════════
    //  EXCHANGE
    // ══════════════════════════════════════════════════════

    /**
     * Process an exchange: return some items and take new ones.
     * Net amount = new items total - returned items total.
     * If positive: customer owes the difference.
     * If negative: customer receives the difference as refund.
     */
    @Transactional
    public TransactionResponse processExchange(CreateReturnRequest request,
                                                Long employeeId, Long tenantId) {

        if (request.getExchangeItems() == null || request.getExchangeItems().isEmpty()) {
            throw new BusinessException(
                "Exchange requires at least one exchange item. Use processReturn for plain returns.");
        }

        // 1. Process the return portion
        Transaction original = loadAndValidateOriginal(request.getOriginalTxnId(), tenantId);
        Employee employee = employeeRepository
                .findByEmployeeIdAndTenant_TenantId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        List<ReturnItem> returnItems = new ArrayList<>();
        BigDecimal returnSubtotal = BigDecimal.ZERO;

        for (CreateReturnRequest.ReturnLineRequest lineReq : request.getReturnItems()) {
            TransactionItem originalItem = findAndValidateItem(
                    lineReq.getOriginalItemId(), original, lineReq.getQtyToReturn());

            ReturnReason reason = lineReq.getReasonId() != null
                    ? returnReasonRepository.findById(lineReq.getReasonId()).orElse(null)
                    : null;

            BigDecimal lineRefund = originalItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(lineReq.getQtyToReturn()))
                    .setScale(2, RoundingMode.HALF_UP);

            returnItems.add(ReturnItem.builder()
                    .originalTxnId(original.getTxnId())
                    .originalItem(originalItem)
                    .product(originalItem.getProduct())
                    .qtyReturned(lineReq.getQtyToReturn())
                    .unitPrice(originalItem.getUnitPrice())
                    .refundAmount(lineRefund)
                    .reason(reason)
                    .restock(lineReq.getRestock())
                    .build());

            returnSubtotal = returnSubtotal.add(lineRefund);
        }

        // 2. Calculate new items subtotal
        Map<Product, Integer> newItemsMap = new LinkedHashMap<>();
        for (CreateTransactionRequest.TransactionItemRequest newItem : request.getExchangeItems()) {
            Product product = productRepository.findById(newItem.getProductId())
                    .filter(p -> p.getTenant().getTenantId().equals(tenantId))
                    .filter(Product::getActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", newItem.getProductId()));

            if (product.getStockQty() < newItem.getQty()) {
                throw new BusinessException("Insufficient stock for: " + product.getName());
            }
            newItemsMap.put(product, newItem.getQty());
        }

        BigDecimal newSubtotal = newItemsMap.entrySet().stream()
                .map(e -> e.getKey().getPrice().multiply(BigDecimal.valueOf(e.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Net = new items - returned items
        BigDecimal netAmount = newSubtotal.subtract(returnSubtotal).setScale(2, RoundingMode.HALF_UP);

        // 3. Create EXCHANGE transaction
        Transaction exchangeTxn = Transaction.builder()
                .tenant(original.getTenant())
                .customer(original.getCustomer())
                .employee(employee)
                .receiptNumber(generateReceiptNumber("EXC"))
                .subtotal(netAmount)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)             // simplified: no tax on net exchange
                .total(netAmount)
                .paymentMethod(request.getRefundPaymentMethod())
                .status(TransactionStatus.COMPLETED)
                .txnType(TransactionType.EXCHANGE)
                .originalTxnId(original.getTxnId())
                .loyaltyPointsEarned(netAmount.compareTo(BigDecimal.ZERO) > 0
                        ? netAmount.intValue() : 0)
                .notes(request.getNotes())
                .completedAt(LocalDateTime.now())
                .build();

        exchangeTxn = transactionRepository.save(exchangeTxn);

        // 4. Save return items and restock
        for (ReturnItem ri : returnItems) {
            ri.setReturnTransaction(exchangeTxn);
            returnItemRepository.save(ri);

            if (Boolean.TRUE.equals(ri.getRestock())) {
                Product product = ri.getProduct();
                int before = product.getStockQty();
                product.setStockQty(before + ri.getQtyReturned());
                productRepository.save(product);

                adjustmentRepository.save(InventoryAdjustment.builder()
                        .tenantId(tenantId).productId(product.getProductId())
                        .employeeId(employeeId)
                        .adjustmentType(AdjustmentType.RETURN)
                        .qtyBefore(before).qtyChange(ri.getQtyReturned())
                        .qtyAfter(product.getStockQty())
                        .reason("Exchange return on " + exchangeTxn.getReceiptNumber())
                        .referenceId(exchangeTxn.getTxnId()).build());
            }
        }

        // 5. Decrement stock for new items
        for (Map.Entry<Product, Integer> entry : newItemsMap.entrySet()) {
            Product product = entry.getKey();
            int qty = entry.getValue();
            product.setStockQty(product.getStockQty() - qty);
            productRepository.save(product);
        }

        // 6. Update original if fully returned
        if (isFullyReturned(original)) {
            original.setStatus(TransactionStatus.REFUNDED);
            transactionRepository.save(original);
        }

        auditService.log(tenantId, employeeId, employee.getFullName(),
                "EXCHANGE", "TRANSACTION", exchangeTxn.getTxnId(),
                "Exchange for original TXN " + original.getReceiptNumber() +
                " | net=" + netAmount);

        log.info("Exchange {} processed | returned={} items, new={} items | net=${}",
                exchangeTxn.getReceiptNumber(), returnItems.size(),
                newItemsMap.size(), netAmount);

        return toResponse(exchangeTxn, returnItems);
    }

    // ══════════════════════════════════════════════════════
    //  Return Reasons management
    // ══════════════════════════════════════════════════════

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ReturnReason> getReturnReasons(Long tenantId) {
        return returnReasonRepository
                .findByTenant_TenantIdAndActiveTrueOrderBySortOrderAsc(tenantId);
    }

    @Transactional
    public ReturnReason createReturnReason(ReturnReason reason, Long tenantId) {
        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        reason.setTenant(tenant);
        return returnReasonRepository.save(reason);
    }

    // ══════════════════════════════════════════════════════
    //  Private helpers
    // ══════════════════════════════════════════════════════

    private Transaction loadAndValidateOriginal(Long txnId, Long tenantId) {
        Transaction txn = transactionRepository.findById(txnId)
                .filter(t -> t.getTenant().getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", txnId));

        if (txn.getStatus() != TransactionStatus.COMPLETED
                && txn.getStatus() != TransactionStatus.REFUNDED) {
            throw new BusinessException(
                "Only COMPLETED transactions can be returned. Status: " + txn.getStatus());
        }
        if (txn.getTxnType() != TransactionType.SALE) {
            throw new BusinessException("Only SALE transactions can be returned.");
        }
        return txn;
    }

    private TransactionItem findAndValidateItem(Long itemId, Transaction original, int qtyToReturn) {
        TransactionItem item = original.getItems().stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                    "Item " + itemId + " is not part of transaction " + original.getReceiptNumber()));

        // How many have already been returned?
        int alreadyReturned = returnItemRepository.sumReturnedQtyForItem(itemId);
        int returnable = item.getQty() - alreadyReturned;

        if (qtyToReturn > returnable) {
            throw new BusinessException(
                "Cannot return " + qtyToReturn + " of " + item.getProduct().getName() +
                ". Max returnable: " + returnable +
                " (" + alreadyReturned + " already returned).");
        }
        return item;
    }

    private boolean isFullyReturned(Transaction original) {
        return original.getItems().stream().allMatch(item -> {
            int returned = returnItemRepository.sumReturnedQtyForItem(item.getItemId());
            return returned >= item.getQty();
        });
    }

    private String generateReceiptNumber(String prefix) {
        String date = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rand = String.format("%04d", new java.util.Random().nextInt(10000));
        return prefix + "-" + date + "-" + rand;
    }

    private TransactionResponse toResponse(Transaction t, List<ReturnItem> returnItems) {
        List<TransactionResponse.LineItemResponse> lines = returnItems.stream()
                .map(ri -> TransactionResponse.LineItemResponse.builder()
                        .productId(ri.getProduct().getProductId())
                        .productName(ri.getProduct().getName())
                        .sku(ri.getProduct().getSku())
                        .qty(ri.getQtyReturned())
                        .unitPrice(ri.getUnitPrice())
                        .discountApplied(BigDecimal.ZERO)
                        .lineTotal(ri.getRefundAmount().negate())
                        .build())
                .toList();

        return TransactionResponse.builder()
                .txnId(t.getTxnId())
                .receiptNumber(t.getReceiptNumber())
                .employeeName(t.getEmployee().getFullName())
                .items(lines)
                .subtotal(t.getSubtotal())
                .discountAmount(t.getDiscountAmount())
                .taxAmount(t.getTaxAmount())
                .total(t.getTotal())
                .paymentMethod(t.getPaymentMethod())
                .status(t.getStatus())
                .txnType(t.getTxnType())
                .loyaltyPointsEarned(t.getLoyaltyPointsEarned())
                .completedAt(t.getCompletedAt())
                .build();
    }

}
