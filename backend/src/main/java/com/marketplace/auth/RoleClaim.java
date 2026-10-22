package com.marketplace.auth;

import com.marketplace.domain.RoleType;

import java.util.UUID;

public record RoleClaim(RoleType role, UUID tenantId, String tenantName) {
}
