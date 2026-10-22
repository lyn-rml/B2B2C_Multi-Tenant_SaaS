package com.marketplace.repository;

import com.marketplace.domain.TenantModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantModuleRepository extends JpaRepository<TenantModuleEntity, Long> {

    List<TenantModuleEntity> findByTenant_IdOrderByModuleKeyAsc(UUID tenantId);
}
