package com.universalpos.repository;

import com.universalpos.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmailAndTenant_TenantSlugAndActiveTrue(
            String email, String tenantSlug);

    Optional<Employee> findByEmployeeIdAndTenant_TenantId(
            Long employeeId, Long tenantId);

    @Query("SELECT e FROM Employee e WHERE e.email = :email " +
           "AND e.tenant.tenantId = :tenantId AND e.active = true")
    Optional<Employee> findActiveByEmailAndTenantId(
            @Param("email") String email,
            @Param("tenantId") Long tenantId);
}
