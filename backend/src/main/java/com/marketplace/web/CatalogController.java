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
@Tag(name = "Catalog (products & baskets)")
public class CatalogController {

    private final TenantCatalogService tenantCatalogService;

    public CatalogController(TenantCatalogService tenantCatalogService) {
        this.tenantCatalogService = tenantCatalogService;
    }

    @GetMapping("/addresses/{addressId}/products")
    @PreAuthorize("@tenantGuard.isMemberOf(authentication, #tenantId)")
    @Operation(summary = "List products at an address")
    public List<ProductDto> listProducts(@PathVariable UUID tenantId, @PathVariable Long addressId) {
        return tenantCatalogService.listProducts(tenantId, addressId);
    }

    @PostMapping("/addresses/{addressId}/products")
    @PreAuthorize("@tenantGuard.canManageAddress(authentication, #tenantId, #addressId)")
    @Operation(summary = "Create a product at an address")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(
            @PathVariable UUID tenantId,
            @PathVariable Long addressId,
            @Valid @RequestBody CreateProductRequest request
    ) {
        return tenantCatalogService.createProduct(tenantId, addressId, request);
    }

    @GetMapping("/products/{productId}/batches")
    @PreAuthorize("@tenantGuard.isMemberOf(authentication, #tenantId)")
    @Operation(summary = "List batches for a product")
    public List<ProductBatchDto> listBatches(@PathVariable UUID tenantId, @PathVariable Long productId) {
        return tenantCatalogService.listBatches(tenantId, productId);
    }

    @PostMapping("/products/{productId}/batches")
    @PreAuthorize("@tenantGuard.isMemberOf(authentication, #tenantId)")
    @Operation(summary = "Add a batch to a product")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductBatchDto createBatch(
            @PathVariable UUID tenantId,
            @PathVariable Long productId,
            @Valid @RequestBody CreateProductBatchRequest request
    ) {
        return tenantCatalogService.createBatch(tenantId, productId, request);
    }

    @GetMapping("/addresses/{addressId}/baskets")
    @PreAuthorize("@tenantGuard.isMemberOf(authentication, #tenantId)")
    @Operation(summary = "List baskets at an address")
    public List<BasketDto> listBaskets(@PathVariable UUID tenantId, @PathVariable Long addressId) {
        return tenantCatalogService.listBaskets(tenantId, addressId);
    }

    @PostMapping("/addresses/{addressId}/baskets")
    @PreAuthorize("@tenantGuard.canManageAddress(authentication, #tenantId, #addressId)")
    @Operation(summary = "Create a basket at an address")
    @ResponseStatus(HttpStatus.CREATED)
    public BasketDto createBasket(
            @PathVariable UUID tenantId,
            @PathVariable Long addressId,
            @Valid @RequestBody CreateBasketRequest request
    ) {
        return tenantCatalogService.createBasket(tenantId, addressId, request);
    }

    @PostMapping("/baskets/{basketId}/items")
    @PreAuthorize("@tenantGuard.canManageBasket(authentication, #tenantId, #basketId)")
    @Operation(summary = "Add or update an item in a basket")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addBasketItem(
            @PathVariable UUID tenantId,
            @PathVariable Long basketId,
            @Valid @RequestBody AddBasketItemRequest request
    ) {
        tenantCatalogService.addBasketItem(tenantId, basketId, request);
    }

    @PostMapping("/baskets/{basketId}/publish")
    @PreAuthorize("@tenantGuard.canManageBasket(authentication, #tenantId, #basketId)")
    @Operation(summary = "Publish a basket on a date (reuses process row for same day)")
    public BasketDto publishBasket(
            @PathVariable UUID tenantId,
            @PathVariable Long basketId,
            @Valid @RequestBody PublishRequest request
    ) {
        return tenantCatalogService.publishBasket(tenantId, basketId, request);
    }
}
