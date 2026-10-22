package com.marketplace.security;

import com.marketplace.repository.TenantMembershipRepository;
import com.marketplace.repository.UserRoleRepository;
import com.marketplace.service.TenantSchemaAddressService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("tenantGuard")
public class TenantGuard {

    private final TenantSchemaAddressService tenantSchemaAddressService;
    private final UserRoleRepository userRoleRepository;
    private final TenantMembershipRepository tenantMembershipRepository;

    public TenantGuard(
            TenantSchemaAddressService tenantSchemaAddressService,
            UserRoleRepository userRoleRepository,
            TenantMembershipRepository tenantMembershipRepository
    ) {
        this.tenantSchemaAddressService = tenantSchemaAddressService;
        this.userRoleRepository = userRoleRepository;
        this.tenantMembershipRepository = tenantMembershipRepository;
    }

    public boolean isAdminFor(Authentication authentication, UUID tenantId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Long userId = Long.parseLong(authentication.getName());
        return tenantSchemaAddressService.isTenantAdminFor(userId, tenantId);
    }

    public boolean isMemberOf(Authentication authentication, UUID tenantId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Long userId = Long.parseLong(authentication.getName());
        return userRoleRepository.existsByUser_IdAndTenant_Id(userId, tenantId)
                || tenantMembershipRepository.existsByUser_IdAndTenant_Id(userId, tenantId);
    }

    public boolean canManageAddress(Authentication authentication, UUID tenantId, Long addressId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Long userId = Long.parseLong(authentication.getName());
        return tenantSchemaAddressService.canManageAddress(userId, tenantId, addressId);
    }

    public boolean canManageBasket(Authentication authentication, UUID tenantId, Long basketId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Long userId = Long.parseLong(authentication.getName());
        return tenantSchemaAddressService.canManageBasket(userId, tenantId, basketId);
    }

    public boolean canManageStudio(Authentication authentication, UUID tenantId, Long studioId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Long userId = Long.parseLong(authentication.getName());
        return tenantSchemaAddressService.canManageStudio(userId, tenantId, studioId);
    }

    public boolean canManageActivity(Authentication authentication, UUID tenantId, Long activityId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Long userId = Long.parseLong(authentication.getName());
        return tenantSchemaAddressService.canManageActivity(userId, tenantId, activityId);
    }
}
