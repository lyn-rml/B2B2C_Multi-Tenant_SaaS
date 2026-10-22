package com.marketplace.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddBasketItemRequest(
        @NotNull Long productBatchId,
        @NotNull BigDecimal quantity
) {
}
