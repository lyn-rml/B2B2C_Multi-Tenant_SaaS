package com.marketplace.web.dto;

import com.marketplace.auth.RoleClaim;

import java.util.List;

public record AuthResponse(
        String token,
        String email,
        String fullName,
        List<RoleClaim> roles
) {
}
