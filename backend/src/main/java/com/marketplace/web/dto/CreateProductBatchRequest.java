package com.marketplace.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProductBatchRequest(
        @NotNull LocalDate expiryDate,
        @NotNull BigDecimal quantity
) {
}
