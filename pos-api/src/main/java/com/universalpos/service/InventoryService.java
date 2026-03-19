package com.universalpos.service;

import com.universalpos.domain.*;
import com.universalpos.domain.PurchaseOrder.PoStatus;
import com.universalpos.exception.BusinessException;
import com.universalpos.exception.ResourceNotFoundException;
import com.universalpos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * InventoryService — manages the full inventory lifecycle:
 *
 *   Suppliers     — create, update, list suppliers
 *   Purchase Orders — create PO, submit, receive items (full or partial)
 *   Manual Adjustments — damage, theft, manual corrections
 *   Stock Counts  — create count session, record counted quantities, apply variances
 *   Low Stock     — find products at or below reorder point
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository              productRepository;
    private final SupplierRepository             supplierRepository;
    private final PurchaseOrderRepository        purchaseOrderRepository;
    private final InventoryAdjustmentRepository  adjustmentRepository;
    private final StockCountRepository           stockCountRepository;
    private final TenantRepository               tenantRepository;
    private final EmployeeRepository             employeeRepository;
    private final AuditService                   auditService;

    // ══════════════════════════════════════════════════════
    //  SUPPLIERS
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers(Long tenantId) {
        return supplierRepository.findByTenant_TenantIdAndActiveTrueOrderByNameAsc(tenantId);
    }

    @Transactional(readOnly = true)
    public Page<Supplier> searchSuppliers(Long tenantId, String term, Pageable pageable) {
        return supplierRepository.searchSuppliers(tenantId, term, pageable);
    }

    @Transactional
    public Supplier createSupplier(Supplier supplier, Long tenantId,
                                   Long employeeId, String employeeName) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        supplier.setTenant(tenant);
        Supplier saved = supplierRepository.save(supplier);

        auditService.log(tenantId, employeeId, employeeName,
                "CREATE_SUPPLIER", "SUPPLIER", saved.getSupplierId(),
                "New supplier: " + saved.getName());

        log.info("Supplier created: {} (id={})", saved.getName(), saved.getSupplierId());
        return saved;
    }

    @Transactional
    public Supplier updateSupplier(Long supplierId, Supplier updates,
                                   Long tenantId, Long employeeId, String employeeName) {
        Supplier existing = supplierRepository.findById(supplierId)
                .filter(s -> s.getTenant().getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));

        if (updates.getName()         != null) existing.setName(updates.getName());
        if (updates.getContactName()  != null) existing.setContactName(updates.getContactName());
        if (updates.getEmail()        != null) existing.setEmail(updates.getEmail());
        if (updates.getPhone()        != null) existing.setPhone(updates.getPhone());
        if (updates.getAddress()      != null) existing.setAddress(updates.getAddress());
        if (updates.getPaymentTerms() != null) existing.setPaymentTerms(updates.getPaymentTerms());
        if (updates.getLeadTimeDays() != null) existing.setLeadTimeDays(updates.getLeadTimeDays());
        if (updates.getNotes()        != null) existing.setNotes(updates.getNotes());
        if (updates.getActive()       != null) existing.setActive(updates.getActive());

        auditService.log(tenantId, employeeId, employeeName,
                "UPDATE_SUPPLIER", "SUPPLIER", supplierId, "Supplier updated");
        return supplierRepository.save(existing);
    }

    // ══════════════════════════════════════════════════════
    //  PURCHASE ORDERS
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<PurchaseOrder> listPurchaseOrders(Long tenantId, Pageable pageable) {
        return purchaseOrderRepository.findByTenant_TenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrder(Long poId, Long tenantId) {
        return purchaseOrderRepository.findById(poId)
                .filter(po -> po.getTenant().getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", poId));
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts(Long tenantId) {
        return productRepository.findAll().stream()
                .filter(p -> p.getTenant().getTenantId().equals(tenantId))
                .filter(Product::getActive)
                .filter(Product::isLowStock)
                .toList();
    }

    /**
     * Create a new purchase order in DRAFT status.
     * Add items to it before submitting.
     */
    @Transactional
    public PurchaseOrder createPurchaseOrder(Long supplierId, LocalDate expectedDate,
                                              String notes, Long tenantId,
                                              Long employeeId, String employeeName) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        Supplier supplier = supplierRepository.findById(supplierId)
                .filter(s -> s.getTenant().getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        PurchaseOrder po = PurchaseOrder.builder()
                .tenant(tenant)
                .supplier(supplier)
                .createdBy(employee)
                .poNumber(generatePoNumber())
                .status(PoStatus.DRAFT)
                .expectedDate(expectedDate)
                .notes(notes)
                .build();

        PurchaseOrder saved = purchaseOrderRepository.save(po);

        auditService.log(tenantId, employeeId, employeeName,
                "CREATE_PO", "PURCHASE_ORDER", saved.getPoId(),
                "PO created: " + saved.getPoNumber() + " → " + supplier.getName());

        log.info("Purchase order {} created for supplier {}", saved.getPoNumber(), supplier.getName());
        return saved;
    }

    /**
     * Add a product line to a DRAFT purchase order.
     */
    @Transactional
    public PurchaseOrder addItemToPurchaseOrder(Long poId, Long productId,
                                                 Integer qty, BigDecimal unitCost,
                                                 Long tenantId) {
        PurchaseOrder po = getPurchaseOrder(poId, tenantId);

        if (!po.isEditable()) {
            throw new BusinessException(
                "Cannot modify purchase order " + po.getPoNumber() +
                " — status is " + po.getStatus() + ". Only DRAFT orders can be edited.");
        }

        Product product = productRepository.findById(productId)
                .filter(p -> p.getTenant().getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .product(product)
                .qtyOrdered(qty)
                .unitCost(unitCost)
                .lineTotal(unitCost.multiply(BigDecimal.valueOf(qty))
                           .setScale(2, RoundingMode.HALF_UP))
                .build();

        po.getItems().add(item);
        recalculateTotals(po);

        return purchaseOrderRepository.save(po);
    }

    /**
     * Submit a DRAFT PO — marks it as sent to the supplier.
     */
    @Transactional
    public PurchaseOrder submitPurchaseOrder(Long poId, Long tenantId,
                                              Long employeeId, String employeeName) {
        PurchaseOrder po = getPurchaseOrder(poId, tenantId);

        if (po.getStatus() != PoStatus.DRAFT) {
            throw new BusinessException("Only DRAFT purchase orders can be submitted.");
        }
        if (po.getItems().isEmpty()) {
            throw new BusinessException("Cannot submit a purchase order with no items.");
        }

        po.setStatus(PoStatus.SUBMITTED);
        po.setOrderDate(LocalDate.now());

        auditService.log(tenantId, employeeId, employeeName,
                "SUBMIT_PO", "PURCHASE_ORDER", poId,
                "PO submitted: " + po.getPoNumber());

        log.info("Purchase order {} submitted", po.getPoNumber());
        return purchaseOrderRepository.save(po);
    }

    /**
     * Record receipt of items from a purchase order.
     * Updates stock quantities and creates adjustment records.
     * Supports partial receipts — call multiple times as shipments arrive.
     *
     * @param receivedQuantities Map of PO item ID → quantity received in this shipment
     */
    @Transactional
    public PurchaseOrder receivePurchaseOrder(Long poId,
                                               java.util.Map<Long, Integer> receivedQuantities,
                                               Long tenantId,
                                               Long employeeId, String employeeName) {
        PurchaseOrder po = getPurchaseOrder(poId, tenantId);

        if (!po.canReceive()) {
            throw new BusinessException(
                "Purchase order " + po.getPoNumber() +
                " cannot receive items — current status: " + po.getStatus());
        }

        for (PurchaseOrderItem item : po.getItems()) {
            Integer qtyReceiving = receivedQuantities.get(item.getItemId());
            if (qtyReceiving == null || qtyReceiving <= 0) continue;

            int outstanding = item.getQtyOutstanding();
            if (qtyReceiving > outstanding) {
                throw new BusinessException(
                    "Cannot receive " + qtyReceiving + " of " +
                    item.getProduct().getName() +
                    " — only " + outstanding + " outstanding.");
            }

            // Update item received quantity
            item.setQtyReceived(item.getQtyReceived() + qtyReceiving);

            // Update product stock
            Product product = item.getProduct();
            int qtyBefore = product.getStockQty();
            product.setStockQty(qtyBefore + qtyReceiving);
            productRepository.save(product);

            // Write inventory adjustment record
            InventoryAdjustment adj = InventoryAdjustment.builder()
                    .tenantId(tenantId)
                    .productId(product.getProductId())
                    .employeeId(employeeId)
                    .adjustmentType(InventoryAdjustment.AdjustmentType.PO_RECEIPT)
                    .qtyBefore(qtyBefore)
                    .qtyChange(qtyReceiving)
                    .qtyAfter(product.getStockQty())
                    .reason("Received on PO " + po.getPoNumber())
                    .referenceId(poId)
                    .build();
            adjustmentRepository.save(adj);

            log.info("Received {} units of {} (stock: {} → {})",
                     qtyReceiving, product.getName(), qtyBefore, product.getStockQty());
        }

        // Update PO status
        boolean allReceived = po.getItems().stream().allMatch(PurchaseOrderItem::isFullyReceived);
        po.setStatus(allReceived ? PoStatus.RECEIVED : PoStatus.PARTIAL);
        if (allReceived) {
            po.setReceivedDate(LocalDate.now());
        }

        auditService.log(tenantId, employeeId, employeeName,
                "RECEIVE_PO", "PURCHASE_ORDER", poId,
                "Items received on PO " + po.getPoNumber() +
                " — status: " + po.getStatus());

        return purchaseOrderRepository.save(po);
    }

    // ══════════════════════════════════════════════════════
    //  MANUAL ADJUSTMENTS
    // ══════════════════════════════════════════════════════

    /**
     * Record a manual stock adjustment (damage, theft, manual correction, etc.)
     */
    @Transactional
    public InventoryAdjustment adjustStock(Long productId,
                                            InventoryAdjustment.AdjustmentType type,
                                            Integer qtyChange,
                                            String reason,
                                            Long tenantId,
                                            Long employeeId, String employeeName) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getTenant().getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        int qtyBefore = product.getStockQty();
        int qtyAfter  = qtyBefore + qtyChange;

        if (qtyAfter < 0) {
            throw new BusinessException(
                "Adjustment would result in negative stock (" + qtyAfter +
                ") for product: " + product.getName());
        }

        product.setStockQty(qtyAfter);
        productRepository.save(product);

        InventoryAdjustment adj = InventoryAdjustment.builder()
                .tenantId(tenantId)
                .productId(productId)
                .employeeId(employeeId)
                .adjustmentType(type)
                .qtyBefore(qtyBefore)
                .qtyChange(qtyChange)
                .qtyAfter(qtyAfter)
                .reason(reason)
                .build();

        InventoryAdjustment saved = adjustmentRepository.save(adj);

        auditService.log(tenantId, employeeId, employeeName,
                "STOCK_ADJUSTMENT", "PRODUCT", productId,
                type + ": " + qtyChange + " units | " + reason);

        log.info("Stock adjustment: {} {} units of {} ({} → {})",
                 type, qtyChange, product.getName(), qtyBefore, qtyAfter);
        return saved;
    }

    // ══════════════════════════════════════════════════════
    //  STOCK COUNTS
    // ══════════════════════════════════════════════════════

    /**
     * Start a new stock count session.
     * Populates count items with current system stock levels.
     */
    @Transactional
    public StockCount startStockCount(StockCount.CountType type, String categoryFilter,
                                       Long tenantId, Long employeeId, String employeeName) {
        // Only one active count at a time
        stockCountRepository.findByTenant_TenantIdAndStatus(tenantId, StockCount.CountStatus.IN_PROGRESS)
                .ifPresent(existing -> {
                    throw new BusinessException(
                        "A stock count is already in progress (ID: " + existing.getCountId() +
                        "). Complete or cancel it before starting a new one.");
                });

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        StockCount count = StockCount.builder()
                .tenant(tenant)
                .createdBy(employee)
                .status(StockCount.CountStatus.IN_PROGRESS)
                .countType(type)
                .categoryFilter(categoryFilter)
                .build();

        // Populate items — snapshot current stock levels
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> p.getTenant().getTenantId().equals(tenantId))
                .filter(Product::getActive)
                .filter(p -> {
                    if (type == StockCount.CountType.CATEGORY && categoryFilter != null) {
                        return categoryFilter.equalsIgnoreCase(p.getCategory());
                    }
                    return true;
                })
                .toList();

        for (Product product : products) {
            StockCountItem item = StockCountItem.builder()
                    .stockCount(count)
                    .product(product)
                    .qtyExpected(product.getStockQty())
                    .build();
            count.getItems().add(item);
        }

        StockCount saved = stockCountRepository.save(count);

        auditService.log(tenantId, employeeId, employeeName,
                "START_STOCK_COUNT", "STOCK_COUNT", saved.getCountId(),
                type + " count started — " + products.size() + " products");

        log.info("Stock count {} started: {} type, {} products",
                 saved.getCountId(), type, products.size());
        return saved;
    }

    /**
     * Complete a stock count — apply corrections for any variances.
     * Creates COUNT_CORRECTION adjustment records and updates product stock.
     */
    @Transactional
    public StockCount completeStockCount(Long countId, Long tenantId,
                                          Long employeeId, String employeeName) {
        StockCount count = stockCountRepository.findById(countId)
                .filter(c -> c.getTenant().getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("StockCount", countId));

        if (count.getStatus() != StockCount.CountStatus.IN_PROGRESS) {
            throw new BusinessException("Only IN_PROGRESS stock counts can be completed.");
        }

        int variances = 0;
        for (StockCountItem item : count.getItems()) {
            if (item.getQtyCounted() == null) continue; // skip uncounted items
            if (!item.hasVariance()) continue;

            // Apply the variance correction
            Product product = item.getProduct();
            int qtyBefore = product.getStockQty();
            int qtyAfter  = item.getQtyCounted();

            product.setStockQty(qtyAfter);
            productRepository.save(product);

            InventoryAdjustment adj = InventoryAdjustment.builder()
                    .tenantId(tenantId)
                    .productId(product.getProductId())
                    .employeeId(employeeId)
                    .adjustmentType(InventoryAdjustment.AdjustmentType.COUNT_CORRECTION)
                    .qtyBefore(qtyBefore)
                    .qtyChange(item.getVariance())
                    .qtyAfter(qtyAfter)
                    .reason("Stock count #" + countId + " variance correction")
                    .referenceId(countId)
                    .build();
            adjustmentRepository.save(adj);
            variances++;
        }

        count.setStatus(StockCount.CountStatus.COMPLETED);
        count.setCompletedAt(LocalDateTime.now());
        StockCount saved = stockCountRepository.save(count);

        auditService.log(tenantId, employeeId, employeeName,
                "COMPLETE_STOCK_COUNT", "STOCK_COUNT", countId,
                "Count completed — " + variances + " variance corrections applied");

        log.info("Stock count {} completed — {} variances corrected", countId, variances);
        return saved;
    }

    // ── Private helpers ──────────────────────────────────────────

    private void recalculateTotals(PurchaseOrder po) {
        BigDecimal subtotal = po.getItems().stream()
                .map(PurchaseOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        po.setSubtotal(subtotal);
        po.setTotal(subtotal.add(po.getTaxAmount()).add(po.getShippingCost()));
    }

    private String generatePoNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rand = String.format("%04d", new Random().nextInt(10000));
        return "PO-" + date + "-" + rand;
    }
}
