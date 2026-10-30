package com.marketplace.auth;

import com.marketplace.domain.RoleType;
import com.marketplace.domain.TenantEntity;
import com.marketplace.domain.UserRoleEntity;
import com.marketplace.repository.UserRoleRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String HEADER_ACTIVE_ROLE = "X-Active-Role";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    private final UserRoleRepository userRoleRepository;

    public TenantContextFilter(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof String principal) {
                Long userId = Long.parseLong(principal);
                if (requiresTenantContext(request)) {
                    Set<String> authorities = authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());

                    RoleType activeRole = resolveActiveRole(request, authorities);
                    UUID tenantId = resolveTenantId(request, activeRole, authorities, userId);

                    TenantContextHolder.set(new TenantContext(userId, activeRole, tenantId));
                } else {
                    TenantContextHolder.set(new TenantContext(userId, null, null));
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private RoleType resolveActiveRole(HttpServletRequest request, Set<String> authorities) {
        if (request.getRequestURI().startsWith("/api/student/")) {
            requireAuthority(authorities, RoleType.STUDENT);
            return RoleType.STUDENT;
        }

        String header = request.getHeader(HEADER_ACTIVE_ROLE);
        if (header == null || header.isBlank()) {
            if (authorities.size() == 1) {
                return RoleType.valueOf(authorities.iterator().next());
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Header " + HEADER_ACTIVE_ROLE + " required when user has multiple roles"
            );
        }

        RoleType role = RoleType.valueOf(header.trim());
        requireAuthority(authorities, role);
        return role;
    }

    private UUID resolveTenantId(
            HttpServletRequest request,
            RoleType activeRole,
            Set<String> authorities,
            Long userId
    ) {
        if (activeRole == RoleType.STUDENT || activeRole == RoleType.PLATFORM_ADMIN) {
            return null;
        }

        String tenantHeader = request.getHeader(HEADER_TENANT_ID);
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            try {
                return UUID.fromString(tenantHeader.trim());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + HEADER_TENANT_ID);
            }
        }

        return userRoleRepository.findByUserIdAndRole(userId, activeRole).stream()
                .map(UserRoleEntity::getTenant)
                .filter(t -> t != null)
                .map(TenantEntity::getId)
                .distinct()
                .reduce((a, b) -> {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Header " + HEADER_TENANT_ID + " required when user has multiple tenants for " + activeRole
                    );
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Header " + HEADER_TENANT_ID + " required for staff role " + activeRole
                ));
    }

    private void requireAuthority(Set<String> authorities, RoleType role) {
        if (!authorities.contains(role.name())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role not granted: " + role);
        }
    }

    private boolean requiresTenantContext(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/platform/")
                || uri.startsWith("/api/admin/")
                || uri.startsWith("/api/tenant/")
                || uri.startsWith("/api/store/")
                || uri.startsWith("/api/employee/")
                || uri.startsWith("/api/student/");
    }
}
