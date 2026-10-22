package com.marketplace.web;

import com.marketplace.service.TenantCatalogService;
import com.marketplace.web.dto.OfferType;
import com.marketplace.web.dto.StudentOfferDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student")
@Tag(name = "Student offers")
public class StudentOffersController {

    private final TenantCatalogService tenantCatalogService;

    public StudentOffersController(TenantCatalogService tenantCatalogService) {
        this.tenantCatalogService = tenantCatalogService;
    }

    @GetMapping("/offers")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Shared offers across tenants (filter by tenant and type)")
    public List<StudentOfferDto> offers(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) OfferType type
    ) {
        return tenantCatalogService.listStudentOffers(tenantId, type);
    }
}
