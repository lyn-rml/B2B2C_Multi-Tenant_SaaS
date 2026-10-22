package com.marketplace.web.dto;

import com.marketplace.auth.RoleClaim;

import java.util.List;

public record UserSummaryDto(
        Long id,
        String email,
        String fullName,
        boolean active,
        List<RoleClaim> roles
) {
}
