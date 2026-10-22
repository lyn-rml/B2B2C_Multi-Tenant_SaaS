package com.marketplace.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreatePlanningRequest(
        @NotNull Long userId,
        @NotNull LocalDate planDate,
        @NotBlank String disponibilityJson
) {
}
