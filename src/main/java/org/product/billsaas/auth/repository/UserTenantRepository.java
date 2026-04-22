package org.product.billsaas.auth.repository;

import org.product.billsaas.auth.entity.UserTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserTenantRepository extends JpaRepository<UserTenant, UUID> {

    boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);
}