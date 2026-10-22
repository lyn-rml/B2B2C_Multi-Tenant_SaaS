package com.marketplace.web.dto;

import java.math.BigDecimal;

public record BasketDto(
        Long id,
        Long addressId,
        Long processId,
        String title,
        String description,
        BigDecimal price,
        String status,
        boolean shared,
        boolean active
) {
}
