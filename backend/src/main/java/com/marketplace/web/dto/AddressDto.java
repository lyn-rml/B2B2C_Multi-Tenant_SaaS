package com.marketplace.web.dto;

import java.util.UUID;

public record AddressDto(
        Long id,
        String label,
        String city,
        String streetAddress,
        UUID tenantId,
        String tenantName
) {
}
