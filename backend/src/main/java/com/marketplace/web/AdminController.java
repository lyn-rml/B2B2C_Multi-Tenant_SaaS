package com.marketplace.web;

import com.marketplace.service.AdminService;
import com.marketplace.service.TenantModuleService;
import com.marketplace.web.dto.AddressDto;
import com.marketplace.web.dto.ModuleDto;
import com.marketplace.web.dto.TenantDto;
import com.marketplace.web.dto.UserSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Tenant admin (HR)")
public class AdminController {

    private final AdminService adminService;
    private final TenantModuleService tenantModuleService;

    public AdminController(AdminService adminService, TenantModuleService tenantModuleService) {
        this.adminService = adminService;
        this.tenantModuleService = tenantModuleService;
    }

    @GetMapping("/tenants")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @Operation(summary = "List supermarket tenants you administer")
    public List<TenantDto> tenants(Authentication authentication) {
        return adminService.listTenantsForAdmin(userId(authentication));
    }

    @GetMapping("/tenants/{tenantId}/modules")
    @PreAuthorize("@tenantGuard.isAdminFor(authentication, #tenantId)")
    @Operation(summary = "Enabled modules for a tenant you administer")
    public List<ModuleDto> modules(@PathVariable UUID tenantId, Authentication authentication) {
        return tenantModuleService.enabledModulesForAdmin(userId(authentication), tenantId);
    }

    @GetMapping("/addresses")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @Operation(summary = "List addresses in your tenant scope only")
    public List<AddressDto> addresses(Authentication authentication) {
        return adminService.listAddressesForAdmin(userId(authentication));
    }

    @GetMapping("/stores")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @Operation(summary = "Deprecated alias for /addresses")
    public List<AddressDto> stores(Authentication authentication) {
        return adminService.listAddressesForAdmin(userId(authentication));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @Operation(summary = "List users in your tenant scope (HR dashboard)")
    public List<UserSummaryDto> users(Authentication authentication) {
        return adminService.listUsersForAdmin(userId(authentication));
    }

    private static Long userId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
