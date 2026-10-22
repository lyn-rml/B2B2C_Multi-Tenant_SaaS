package com.marketplace.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PublishRequest(@NotNull LocalDate publicDate) {
}
