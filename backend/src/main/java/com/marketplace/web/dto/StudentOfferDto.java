package com.marketplace.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentOfferDto(
        OfferType type,
        Long id,
        UUID tenantId,
        String tenantName,
        String title,
        String subtitle,
        BigDecimal price,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {
}
