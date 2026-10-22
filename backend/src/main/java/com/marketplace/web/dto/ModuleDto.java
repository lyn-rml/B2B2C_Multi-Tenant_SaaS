package com.marketplace.web.dto;

import com.marketplace.domain.ModuleKey;

import java.time.OffsetDateTime;

public record ModuleDto(ModuleKey moduleKey, OffsetDateTime enabledAt) {
}
