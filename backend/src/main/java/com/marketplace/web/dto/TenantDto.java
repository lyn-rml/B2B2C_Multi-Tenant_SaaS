package com.marketplace.web.dto;

import com.marketplace.domain.TenantType;

import java.util.UUID;

public record TenantDto(UUID id, String name, String schemaName, TenantType tenantType) {
}
