package com.marketplace.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component("tenantGuard")
public class TenantGuard {
    private final JdbcTemplate jdbc;
    public TenantGuard(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isMemberOf(Authentication authentication, UUID tenantId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM public.tenant_memberships m
                    JOIN public.users u ON u.id = m.user_id AND u.is_active
                    JOIN public.user_roles r ON r.user_id = m.user_id AND r.tenant_id = m.tenant_id
                    WHERE m.user_id = ? AND m.tenant_id = ?
                      AND r.role IN ('TENANT_ADMIN', 'STORE_ADMIN', 'EMPLOYEE')
                )
                """, Boolean.class, Long.parseLong(authentication.getName()), tenantId));
    }
}
