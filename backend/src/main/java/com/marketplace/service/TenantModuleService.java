package com.marketplace.service;

import com.marketplace.domain.TenantModuleEntity;
import com.marketplace.repository.TenantModuleRepository;
import com.marketplace.web.dto.ModuleDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class TenantModuleService {

    private final TenantModuleRepository tenantModuleRepository;
    private final TenantSchemaAddressService tenantSchemaAddressService;

    public TenantModuleService(
            TenantModuleRepository tenantModuleRepository,
            TenantSchemaAddressService tenantSchemaAddressService
    ) {
        this.tenantModuleRepository = tenantModuleRepository;
        this.tenantSchemaAddressService = tenantSchemaAddressService;
    }

    @Transactional(readOnly = true)
    public List<ModuleDto> enabledModules(UUID tenantId) {
        return tenantModuleRepository.findByTenant_IdOrderByModuleKeyAsc(tenantId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ModuleDto> enabledModulesForAdmin(Long userId, UUID tenantId) {
        if (!tenantSchemaAddressService.isTenantAdminFor(userId, tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not tenant admin for this chain");
        }
        return enabledModules(tenantId);
    }

    private ModuleDto toDto(TenantModuleEntity entity) {
        return new ModuleDto(entity.getModuleKey(), entity.getEnabledAt());
    }
}
