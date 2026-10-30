package com.marketplace.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentOfferDto(
        OfferType type,
        Long id,
        UUID tenantId,
        String tenantName,
        String title,
        String subtitle,
        BigDecimal price,
        String text,
        String imagePath
) {
}
