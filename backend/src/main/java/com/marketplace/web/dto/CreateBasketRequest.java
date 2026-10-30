package com.marketplace.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record CreateBasketRequest(
        @NotBlank String title,
        String description,
        @NotNull @DecimalMin("0") BigDecimal price
) {
}
