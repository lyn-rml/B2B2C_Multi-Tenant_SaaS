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
@Tag(name = "Activities")
public class ActivityController {

    private final TenantCatalogService tenantCatalogService;

    public ActivityController(TenantCatalogService tenantCatalogService) {
        this.tenantCatalogService = tenantCatalogService;
    }

    @GetMapping("/addresses/{addressId}/activities")
    @PreAuthorize("@tenantGuard.isMemberOf(authentication, #tenantId)")
    @Operation(summary = "List activities at an address")
    public List<ActivityDto> listActivities(@PathVariable UUID tenantId, @PathVariable Long addressId) {
        return tenantCatalogService.listActivities(tenantId, addressId);
    }

    @PostMapping("/addresses/{addressId}/activities")
    @PreAuthorize("@tenantGuard.canManageAddress(authentication, #tenantId, #addressId)")
    @Operation(summary = "Create an activity")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityDto createActivity(
            @PathVariable UUID tenantId,
            @PathVariable Long addressId,
            @Valid @RequestBody CreateActivityRequest request
    ) {
        return tenantCatalogService.createActivity(tenantId, addressId, request);
    }

    @PostMapping("/activities/{activityId}/publish")
    @PreAuthorize("@tenantGuard.canManageActivity(authentication, #tenantId, #activityId)")
    @Operation(summary = "Publish an activity on a date")
    public ActivityDto publishActivity(
            @PathVariable UUID tenantId,
            @PathVariable Long activityId,
            @Valid @RequestBody PublishRequest request
    ) {
        return tenantCatalogService.publishActivity(tenantId, activityId, request);
    }
}
