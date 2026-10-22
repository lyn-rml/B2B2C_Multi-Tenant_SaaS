package com.marketplace.auth;

import com.marketplace.domain.RoleType;

import java.util.UUID;

public record TenantContext(Long userId, RoleType activeRole, UUID activeTenantId) {
}
