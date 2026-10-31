package com.marketplace.web;

import com.marketplace.service.PlatformService;
import com.marketplace.web.dto.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/platform")
@Tag(name = "Platform administration", description = "US-000 and US-001; requires PLATFORM_ADMIN")
public class PlatformController {
    private final PlatformService service;
    public PlatformController(PlatformService service) { this.service = service; }

    @PostMapping("/tenants")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantDto createTenant(@Valid @RequestBody CreateTenantRequest request) { return service.createTenant(request); }

    @GetMapping("/tenants")
    public List<TenantDto> tenants() { return service.listTenants(); }

    @GetMapping("/tenants/{id}")
    public TenantDetails tenant(@PathVariable UUID id) { return service.tenant(id); }

    @PostMapping("/modules")
    @ResponseStatus(HttpStatus.CREATED)
    public ModuleDto createModule(@Valid @RequestBody CreateModuleRequest request) { return service.createModule(request); }

    @GetMapping("/modules")
    public List<ModuleDto> modules() { return service.listModules(); }

    @GetMapping("/tenants/{id}/modules")
    public List<ModuleDto> tenantModules(@PathVariable UUID id) { return service.tenantModules(id); }

    @PostMapping("/module-assignments")
    public List<TenantDetails> assignModules(@Valid @RequestBody AssignModulesRequest request) { return service.assignModules(request); }
}
