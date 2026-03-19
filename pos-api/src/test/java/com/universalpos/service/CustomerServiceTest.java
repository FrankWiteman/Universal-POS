package com.universalpos.service;

import com.universalpos.domain.Customer;
import com.universalpos.domain.Customer.LoyaltyTier;
import com.universalpos.domain.Tenant;
import com.universalpos.dto.request.CreateCustomerRequest;
import com.universalpos.dto.response.CustomerResponse;
import com.universalpos.exception.BusinessException;
import com.universalpos.repository.CustomerRepository;
import com.universalpos.repository.TenantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock TenantRepository   tenantRepository;
    @Mock AuditService       auditService;

    @InjectMocks CustomerService customerService;

    @Test
    @DisplayName("Creating a customer should save with NONE loyalty tier")
    void createCustomer_shouldStartWithNoTier() {
        Tenant tenant = Tenant.builder().tenantId(1L).companyName("Demo").build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(customerRepository.findByEmailAndTenant_TenantIdAndActiveTrue(any(), any()))
                .thenReturn(Optional.empty());
        when(customerRepository.findByLoyaltyCardNumberAndActiveTrue(any()))
                .thenReturn(Optional.empty());
        when(customerRepository.save(any())).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setCustomerId(42L);
            return c;
        });

        CreateCustomerRequest req = new CreateCustomerRequest();
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setEmail("jane@test.com");

        CustomerResponse resp = customerService.create(req, 1L, 1L, "Admin");

        assertThat(resp.getLoyaltyTier()).isEqualTo(LoyaltyTier.NONE);
        assertThat(resp.getLoyaltyPoints()).isEqualTo(0);
        assertThat(resp.getFullName()).isEqualTo("Jane Doe");
    }

    @Test
    @DisplayName("Duplicate email within tenant should throw BusinessException")
    void createCustomer_duplicateEmail_shouldThrow() {
        Customer existing = Customer.builder().customerId(1L).build();
        when(customerRepository.findByEmailAndTenant_TenantIdAndActiveTrue("jane@test.com", 1L))
                .thenReturn(Optional.of(existing));

        CreateCustomerRequest req = new CreateCustomerRequest();
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setEmail("jane@test.com");

        assertThatThrownBy(() -> customerService.create(req, 1L, 1L, "Admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Adding points should upgrade loyalty tier at threshold")
    void addLoyaltyPoints_shouldUpgradeTier() {
        Customer customer = Customer.builder()
                .customerId(1L)
                .firstName("Bob")
                .lastName("Smith")
                .loyaltyTier(LoyaltyTier.NONE)
                .loyaltyPoints(450)
                .active(true)
                .tenant(Tenant.builder().tenantId(1L).build())
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 450 + 60 = 510 → crosses the BRONZE threshold of 500
        customerService.addLoyaltyPoints(1L, 1L, 60);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());

        assertThat(captor.getValue().getLoyaltyTier()).isEqualTo(LoyaltyTier.BRONZE);
        assertThat(captor.getValue().getLoyaltyPoints()).isEqualTo(510);
    }

    @Test
    @DisplayName("LoyaltyTier.fromPoints should return correct tier")
    void loyaltyTier_fromPoints_shouldBeCorrect() {
        assertThat(LoyaltyTier.fromPoints(0)).isEqualTo(LoyaltyTier.NONE);
        assertThat(LoyaltyTier.fromPoints(499)).isEqualTo(LoyaltyTier.NONE);
        assertThat(LoyaltyTier.fromPoints(500)).isEqualTo(LoyaltyTier.BRONZE);
        assertThat(LoyaltyTier.fromPoints(1500)).isEqualTo(LoyaltyTier.SILVER);
        assertThat(LoyaltyTier.fromPoints(5000)).isEqualTo(LoyaltyTier.GOLD);
        assertThat(LoyaltyTier.fromPoints(10000)).isEqualTo(LoyaltyTier.PLATINUM);
        assertThat(LoyaltyTier.fromPoints(99999)).isEqualTo(LoyaltyTier.PLATINUM);
    }
}
