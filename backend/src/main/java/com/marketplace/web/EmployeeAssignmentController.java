package com.marketplace.web;

import com.marketplace.service.TenantCatalogService;
import com.marketplace.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Employee assignments")
public class EmployeeAssignmentController {

    private final TenantCatalogService tenantCatalogService;

    public EmployeeAssignmentController(TenantCatalogService tenantCatalogService) {
        this.tenantCatalogService = tenantCatalogService;
    }

    @GetMapping("/api/tenant/{tenantId}/addresses/{addressId}/employees")
    @PreAuthorize("@tenantGuard.canManageAddress(authentication, #tenantId, #addressId)")
    @Operation(summary = "Employees assigned to a store")
    public List<EmployeeAssignmentDto> listEmployees(
            @PathVariable UUID tenantId,
            @PathVariable Long addressId
    ) {
        return tenantCatalogService.listEmployeesAtAddress(tenantId, addressId);
    }

    @PostMapping("/api/tenant/{tenantId}/addresses/{addressId}/employees")
    @PreAuthorize("@tenantGuard.canManageAddress(authentication, #tenantId, #addressId)")
    @Operation(summary = "Assign an employee to a store")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignEmployee(
            @PathVariable UUID tenantId,
            @PathVariable Long addressId,
            @Valid @RequestBody AssignEmployeeRequest request
    ) {
        tenantCatalogService.assignEmployee(tenantId, addressId, request);
    }

}
