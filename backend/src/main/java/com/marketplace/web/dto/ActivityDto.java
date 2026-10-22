package com.marketplace.web.dto;

import java.time.OffsetDateTime;

public record ActivityDto(
        Long id,
        Long addressId,
        Long processId,
        String title,
        String description,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String status,
        boolean shared,
        boolean active
) {
}
