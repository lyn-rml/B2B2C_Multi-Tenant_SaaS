package com.marketplace.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateStudioRequest(
        @NotBlank String label,
        BigDecimal surfaceM2,
        BigDecimal monthlyRent
) {
}
