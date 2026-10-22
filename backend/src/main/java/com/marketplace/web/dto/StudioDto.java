package com.marketplace.web.dto;

import java.math.BigDecimal;

public record StudioDto(
        Long id,
        Long addressId,
        Long processId,
        String label,
        BigDecimal surfaceM2,
        BigDecimal monthlyRent,
        String status,
        boolean shared,
        boolean active
) {
}
