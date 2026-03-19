package com.universalpos.service;

import com.universalpos.domain.Customer;
import com.universalpos.domain.Customer.LoyaltyTier;
import com.universalpos.dto.request.CreateCustomerRequest;
import com.universalpos.dto.response.CustomerResponse;
import com.universalpos.exception.BusinessException;
import com.universalpos.exception.ResourceNotFoundException;
import com.universalpos.repository.CustomerRepository;
import com.universalpos.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TenantRepository   tenantRepository;
    private final AuditService       auditService;

    /** Search by phone, email, name, or loyalty card number */
    @Transactional(readOnly = true)
    public Page<CustomerResponse> search(Long tenantId, String term, Pageable pageable) {
        return customerRepository.searchCustomers(tenantId, term, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long customerId, Long tenantId) {
        Customer customer = findCustomerForTenant(customerId, tenantId);
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request, Long tenantId,
                                   Long employeeId, String employeeName) {
        // Validate email uniqueness within tenant
        if (request.getEmail() != null) {
            customerRepository.findByEmailAndTenant_TenantIdAndActiveTrue(
                    request.getEmail(), tenantId)
                    .ifPresent(c -> {
                        throw new BusinessException(
                            "A customer with email " + request.getEmail() + " already exists.");
                    });
        }

        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        Customer customer = Customer.builder()
                .tenant(tenant)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .loyaltyCardNumber(generateLoyaltyCardNumber())
                .loyaltyTier(LoyaltyTier.NONE)
                .loyaltyPoints(0)
                .dateOfBirth(request.getDateOfBirth())
                .emailOptIn(request.getEmailOptIn())
                .notes(request.getNotes())
                .build();

        customer = customerRepository.save(customer);

        auditService.log(tenantId, employeeId, employeeName,
                "CREATE_CUSTOMER", "CUSTOMER", customer.getCustomerId(),
                "New customer: " + customer.getFullName());

        log.info("New customer created: {} (id={})", customer.getFullName(), customer.getCustomerId());
        return toResponse(customer);
    }

    /**
     * Add loyalty points to a customer and recalculate their tier.
     * Called automatically after a completed transaction.
     */
    @Transactional
    public void addLoyaltyPoints(Long customerId, Long tenantId, int points) {
        if (points <= 0) return;

        Customer customer = findCustomerForTenant(customerId, tenantId);
        int newTotal = customer.getLoyaltyPoints() + points;
        LoyaltyTier newTier = LoyaltyTier.fromPoints(newTotal);

        boolean tieredUp = newTier.ordinal() > customer.getLoyaltyTier().ordinal();

        customer.setLoyaltyPoints(newTotal);
        customer.setLoyaltyTier(newTier);
        customerRepository.save(customer);

        if (tieredUp) {
            log.info("Customer {} upgraded to {} tier ({}pts)",
                     customer.getFullName(), newTier, newTotal);
        }
    }

    // ── Private helpers ──────────────────────────────────────────

    private Customer findCustomerForTenant(Long customerId, Long tenantId) {
        return customerRepository.findById(customerId)
                .filter(c -> c.getTenant().getTenantId().equals(tenantId))
                .filter(Customer::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    private String generateLoyaltyCardNumber() {
        // Generates a unique 12-digit loyalty card number
        String candidate;
        do {
            candidate = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()))
                    .substring(0, 12);
        } while (customerRepository.findByLoyaltyCardNumberAndActiveTrue(candidate).isPresent());
        return candidate;
    }

    private CustomerResponse toResponse(Customer c) {
        // Calculate points needed for next tier
        LoyaltyTier[] tiers = LoyaltyTier.values();
        int currentOrdinal = c.getLoyaltyTier().ordinal();
        LoyaltyTier nextTier = currentOrdinal < tiers.length - 1
                ? tiers[currentOrdinal + 1] : c.getLoyaltyTier();
        int pointsToNext = nextTier == c.getLoyaltyTier()
                ? 0 : nextTier.getPointThreshold() - c.getLoyaltyPoints();

        return CustomerResponse.builder()
                .customerId(c.getCustomerId())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .fullName(c.getFullName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .loyaltyCardNumber(c.getLoyaltyCardNumber())
                .loyaltyTier(c.getLoyaltyTier())
                .loyaltyPoints(c.getLoyaltyPoints())
                .pointsToNextTier(Math.max(0, pointsToNext))
                .nextTier(nextTier)
                .dateOfBirth(c.getDateOfBirth())
                .emailOptIn(c.getEmailOptIn())
                .vip(c.isVip())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
