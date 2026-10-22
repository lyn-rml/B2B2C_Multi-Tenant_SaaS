package com.marketplace.web.dto;

import com.marketplace.domain.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String fullName,
        @NotBlank String password,
        @NotNull RoleType role,
        UUID tenantId
) {
}
