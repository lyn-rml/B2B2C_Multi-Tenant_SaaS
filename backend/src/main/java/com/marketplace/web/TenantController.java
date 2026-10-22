package com.marketplace.web;

import com.marketplace.service.TenantModuleService;
import com.marketplace.web.dto.ModuleDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenant")
@Tag(name = "Tenant")
public class TenantController {

    private final TenantModuleService tenantModuleService;

    public TenantController(TenantModuleService tenantModuleService) {
        this.tenantModuleService = tenantModuleService;
    }

    @GetMapping("/{tenantId}/modules")
    @PreAuthorize("@tenantGuard.isMemberOf(authentication, #tenantId)")
    @Operation(summary = "Enabled modules for a tenant (any member)")
    public List<ModuleDto> modules(@PathVariable UUID tenantId) {
        return tenantModuleService.enabledModules(tenantId);
    }
}
