package com.marketplace.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateBasketRequest(
        @NotBlank String title,
        String description,
        BigDecimal price
) {
}
