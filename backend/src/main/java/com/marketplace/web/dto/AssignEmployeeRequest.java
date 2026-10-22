package com.marketplace.web.dto;

import jakarta.validation.constraints.NotNull;

public record AssignEmployeeRequest(@NotNull Long userId) {
}
