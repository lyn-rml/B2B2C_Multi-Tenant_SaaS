package com.marketplace.web.dto;

import jakarta.validation.constraints.*;

public record CreateModuleRequest(@NotBlank @Size(max = 150) String name,
                                  @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,39}") String code,
                                  @NotBlank @Size(max = 10000) String description) {}
