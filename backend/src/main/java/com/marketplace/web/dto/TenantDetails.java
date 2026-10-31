package com.marketplace.web.dto;

import java.util.List;

public record TenantDetails(TenantDto tenant, List<ModuleDto> modules) {}
