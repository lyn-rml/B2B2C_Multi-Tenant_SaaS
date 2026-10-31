package com.marketplace.web.dto;

import java.util.UUID;

public record TenantDto(UUID id, String name, String schemaName, String status) {}
