package com.marketplace.web.dto;

import java.time.LocalDate;

public record PlanningEntryDto(
        Long id,
        Long addressId,
        Long userId,
        String userEmail,
        String userFullName,
        LocalDate planDate,
        String disponibilityJson,
        boolean active
) {
}
