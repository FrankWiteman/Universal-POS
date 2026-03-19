package com.universalpos.repository;

import com.universalpos.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByTenantSlugAndActiveTrue(String tenantSlug);

    boolean existsByTenantSlug(String tenantSlug);
}
