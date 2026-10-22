package com.marketplace.auth;

import com.marketplace.domain.RoleType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext context) {
        CONTEXT.set(context);
    }

    public static TenantContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static void requireActiveRole(RoleType role) {
        TenantContext ctx = CONTEXT.get();
        if (ctx == null || ctx.activeRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active role must be " + role);
        }
    }
}
