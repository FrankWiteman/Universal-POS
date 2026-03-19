package com.universalpos.service;

import com.universalpos.domain.Product;
import com.universalpos.domain.Tenant;
import com.universalpos.exception.BusinessException;
import com.universalpos.exception.ResourceNotFoundException;
import com.universalpos.repository.ProductRepository;
import com.universalpos.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final TenantRepository  tenantRepository;
    private final AuditService      auditService;

    @Transactional(readOnly = true)
    public Page<Product> search(Long tenantId, String term, Pageable pageable) {
        return productRepository.searchProducts(tenantId, term, pageable);
    }

    @Transactional(readOnly = true)
    public Product getById(Long productId, Long tenantId) {
        return productRepository.findById(productId)
                .filter(p -> p.getTenant().getTenantId().equals(tenantId))
                .filter(Product::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    @Transactional(readOnly = true)
    public Product getByBarcode(String barcode, Long tenantId) {
        return productRepository.findByBarcodeAndTenant_TenantIdAndActiveTrue(barcode, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "barcode", barcode));
    }

    @Transactional
    public Product create(Product product, Long tenantId,
                          Long employeeId, String employeeName) {
        // Check SKU uniqueness
        productRepository.findBySkuAndTenant_TenantIdAndActiveTrue(product.getSku(), tenantId)
                .ifPresent(p -> {
                    throw new BusinessException("A product with SKU " + product.getSku() + " already exists.");
                });

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        product.setTenant(tenant);
        product.setActive(true);

        Product saved = productRepository.save(product);
        auditService.log(tenantId, employeeId, employeeName,
                "CREATE_PRODUCT", "PRODUCT", saved.getProductId(),
                "New product: " + saved.getName() + " (SKU: " + saved.getSku() + ")");

        log.info("Product created: {} SKU={}", saved.getName(), saved.getSku());
        return saved;
    }

    @Transactional
    public Product update(Long productId, Product updates, Long tenantId,
                          Long employeeId, String employeeName) {
        Product existing = getById(productId, tenantId);

        Optional.ofNullable(updates.getName()).ifPresent(existing::setName);
        Optional.ofNullable(updates.getDescription()).ifPresent(existing::setDescription);
        Optional.ofNullable(updates.getPrice()).ifPresent(existing::setPrice);
        Optional.ofNullable(updates.getCost()).ifPresent(existing::setCost);
        Optional.ofNullable(updates.getStockQty()).ifPresent(existing::setStockQty);
        Optional.ofNullable(updates.getReorderPoint()).ifPresent(existing::setReorderPoint);
        Optional.ofNullable(updates.getCategory()).ifPresent(existing::setCategory);
        Optional.ofNullable(updates.getSubcategory()).ifPresent(existing::setSubcategory);
        Optional.ofNullable(updates.getBrand()).ifPresent(existing::setBrand);
        Optional.ofNullable(updates.getBarcode()).ifPresent(existing::setBarcode);
        Optional.ofNullable(updates.getActive()).ifPresent(existing::setActive);
        Optional.ofNullable(updates.getImageUrl()).ifPresent(existing::setImageUrl);

        Product saved = productRepository.save(existing);
        auditService.log(tenantId, employeeId, employeeName,
                "UPDATE_PRODUCT", "PRODUCT", productId, "Product updated");
        return saved;
    }

    @Transactional
    public void deactivate(Long productId, Long tenantId, Long employeeId, String employeeName) {
        Product product = getById(productId, tenantId);
        product.setActive(false);
        productRepository.save(product);
        auditService.log(tenantId, employeeId, employeeName,
                "DEACTIVATE_PRODUCT", "PRODUCT", productId, "Product deactivated");
    }
}
