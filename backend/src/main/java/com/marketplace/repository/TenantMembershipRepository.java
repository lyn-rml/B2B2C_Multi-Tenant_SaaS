package com.marketplace.repository;

import com.marketplace.domain.TenantMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantMembershipRepository extends JpaRepository<TenantMembershipEntity, Long> {

    boolean existsByUser_IdAndTenant_Id(Long userId, java.util.UUID tenantId);
}
