package com.marketplace.web;

import com.marketplace.service.TenantCatalogService;
import com.marketplace.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenant/{tenantId}")
@Tag(name = "Studios")
public class StudioController {

    private final TenantCatalogService tenantCatalogService;

    public StudioController(TenantCatalogService tenantCatalogService) {
        this.tenantCatalogService = tenantCatalogService;
    }

    @GetMapping("/addresses/{addressId}/studios")
    @PreAuthorize("@tenantGuard.isMemberOf(authentication, #tenantId)")
    @Operation(summary = "List studios at an address")
    public List<StudioDto> listStudios(@PathVariable UUID tenantId, @PathVariable Long addressId) {
        return tenantCatalogService.listStudios(tenantId, addressId);
    }

    @PostMapping("/addresses/{addressId}/studios")
    @PreAuthorize("@tenantGuard.canManageAddress(authentication, #tenantId, #addressId)")
    @Operation(summary = "Create a studio")
    @ResponseStatus(HttpStatus.CREATED)
    public StudioDto createStudio(
            @PathVariable UUID tenantId,
            @PathVariable Long addressId,
            @Valid @RequestBody CreateStudioRequest request
    ) {
        return tenantCatalogService.createStudio(tenantId, addressId, request);
    }

    @PostMapping("/studios/{studioId}/publish")
    @PreAuthorize("@tenantGuard.canManageStudio(authentication, #tenantId, #studioId)")
    @Operation(summary = "Publish a studio on a date")
    public StudioDto publishStudio(
            @PathVariable UUID tenantId,
            @PathVariable Long studioId,
            @Valid @RequestBody PublishRequest request
    ) {
        return tenantCatalogService.publishStudio(tenantId, studioId, request);
    }
}
