package com.universalpos.repository;

import com.universalpos.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmailAndTenant_TenantIdAndActiveTrue(
            String email, Long tenantId);

    Optional<Customer> findByPhoneAndTenant_TenantIdAndActiveTrue(
            String phone, Long tenantId);

    Optional<Customer> findByLoyaltyCardNumberAndActiveTrue(String loyaltyCardNumber);

    /**
     * Flexible search used by the POS terminal lookup screen.
     * Searches across phone, email, first/last name with a single query.
     */
    @Query("SELECT c FROM Customer c WHERE c.tenant.tenantId = :tenantId " +
           "AND c.active = true " +
           "AND (LOWER(c.phone)     LIKE LOWER(CONCAT('%', :term, '%')) " +
           " OR  LOWER(c.email)     LIKE LOWER(CONCAT('%', :term, '%')) " +
           " OR  LOWER(c.firstName) LIKE LOWER(CONCAT('%', :term, '%')) " +
           " OR  LOWER(c.lastName)  LIKE LOWER(CONCAT('%', :term, '%')) " +
           " OR  c.loyaltyCardNumber = :term)")
    Page<Customer> searchCustomers(
            @Param("tenantId") Long tenantId,
            @Param("term") String term,
            Pageable pageable);
}
