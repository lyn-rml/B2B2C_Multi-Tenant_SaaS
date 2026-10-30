package com.marketplace.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateActivityRequest(@NotBlank String text, @NotBlank String imagePath) {}
