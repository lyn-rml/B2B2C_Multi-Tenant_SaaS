package com.marketplace.web;

import com.marketplace.service.PlatformService;
import com.marketplace.web.dto.TenantDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Tenant access")
public class TenantController {
    private final PlatformService service;
    public TenantController(PlatformService service) { this.service = service; }

    @GetMapping("/{id}")
    @PreAuthorize("@tenantGuard.isMemberOf(authentication, #id)")
    public TenantDetails tenant(@PathVariable UUID id) { return service.tenant(id); }
}
