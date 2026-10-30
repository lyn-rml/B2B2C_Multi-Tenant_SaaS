package com.marketplace.service;

import com.marketplace.auth.RoleClaim;
import com.marketplace.domain.RoleType;
import com.marketplace.domain.TenantEntity;
import com.marketplace.domain.UserEntity;
import com.marketplace.domain.UserRoleEntity;
import com.marketplace.repository.UserRoleRepository;
import com.marketplace.web.dto.AddressDto;
import com.marketplace.web.dto.TenantDto;
import com.marketplace.web.dto.UserSummaryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRoleRepository userRoleRepository;
    private final TenantSchemaAddressService tenantSchemaAddressService;

    public AdminService(UserRoleRepository userRoleRepository, TenantSchemaAddressService tenantSchemaAddressService) {
        this.userRoleRepository = userRoleRepository;
        this.tenantSchemaAddressService = tenantSchemaAddressService;
    }

    @Transactional(readOnly = true)
    public List<TenantDto> listTenantsForAdmin(Long userId) {
        List<TenantEntity> adminTenants = tenantSchemaAddressService.tenantsForRole(userId, RoleType.TENANT_ADMIN);
        if (adminTenants.size() > 1) {
            throw new IllegalStateException("User has TENANT_ADMIN on more than one tenant; data must be fixed");
        }
        return adminTenants.stream()
                .map(this::toTenantDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AddressDto> listAddressesForAdmin(Long userId) {
        return tenantSchemaAddressService.listAllAddressesForTenantAdmin(userId);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> listUsersForAdmin(Long userId) {
        Set<UUID> adminTenantIds = tenantSchemaAddressService.tenantsForRole(userId, RoleType.TENANT_ADMIN).stream()
                .map(TenantEntity::getId)
                .collect(Collectors.toSet());

        if (adminTenantIds.isEmpty()) {
            return List.of();
        }

        List<UserRoleEntity> roles = userRoleRepository.findAllWithUserAndTenant();
        Map<Long, UserSummaryDto> byUser = new LinkedHashMap<>();

        for (UserRoleEntity roleEntity : roles) {
            if (roleEntity.getTenant() == null) {
                continue;
            }
            if (!adminTenantIds.contains(roleEntity.getTenant().getId())) {
                continue;
            }

            UserEntity user = roleEntity.getUser();
            UserSummaryDto existing = byUser.get(user.getId());
            List<RoleClaim> roleClaims = existing != null
                    ? new ArrayList<>(existing.roles())
                    : new ArrayList<>();

            roleClaims.add(new RoleClaim(
                    roleEntity.getRole(),
                    roleEntity.getTenant().getId(),
                    roleEntity.getTenant().getName()
            ));

            byUser.put(user.getId(), new UserSummaryDto(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.isActive(),
                    roleClaims
            ));
        }

        return List.copyOf(byUser.values());
    }

    private TenantDto toTenantDto(TenantEntity tenant) {
        return new TenantDto(tenant.getId(), tenant.getName(), tenant.getSchemaName(), tenant.getStatus());
    }
}
