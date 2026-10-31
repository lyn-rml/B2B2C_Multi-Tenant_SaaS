package com.marketplace.web.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record AssignModulesRequest(
        @NotEmpty @Size(max = 100) List<@NotNull UUID> tenantIds,
        @NotEmpty @Size(max = 20) List<@NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,39}") String> moduleCodes) {}
