package com.marketplace.web.dto;

import jakarta.validation.constraints.*;

public record CreateTenantRequest(@NotBlank @Size(max = 150) String name,
                                  @NotNull @Positive Long tenantAdminUserId) {}
