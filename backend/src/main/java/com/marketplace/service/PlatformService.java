package com.marketplace.service;

import com.marketplace.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
public class PlatformService {
    private final JdbcTemplate jdbc;
    private final TenantProvisioningService provisioning;
    private static final RowMapper<TenantDto> TENANT = (rs, row) -> new TenantDto(
            rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("schema_name"), rs.getString("status"));

    public PlatformService(JdbcTemplate jdbc, TenantProvisioningService provisioning) {
        this.jdbc = jdbc;
        this.provisioning = provisioning;
    }

    @Transactional
    public TenantDto createTenant(CreateTenantRequest request) {
        // Serialize competing assignments of the same administrator, before checking its role.
        List<Boolean> active = jdbc.queryForList("SELECT is_active FROM public.users WHERE id = ? FOR UPDATE",
                Boolean.class, request.tenantAdminUserId());
        if (active.isEmpty() || !active.getFirst()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant administrator must be an existing active user");
        }
        if (Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM public.user_roles WHERE user_id = ? AND role = 'TENANT_ADMIN')
                """, Boolean.class, request.tenantAdminUserId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already administers a tenant");
        }
        UUID id = UUID.randomUUID();
        String schema = "tenant_" + id.toString().replace("-", "");
        jdbc.update("INSERT INTO public.tenants (id, name, schema_name, status) VALUES (?, ?, ?, 'PENDING_SETUP')",
                id, request.name().trim(), schema);
        jdbc.update("INSERT INTO public.tenant_memberships (user_id, tenant_id) VALUES (?, ?)", request.tenantAdminUserId(), id);
        jdbc.update("INSERT INTO public.user_roles (user_id, tenant_id, role) VALUES (?, ?, 'TENANT_ADMIN')",
                request.tenantAdminUserId(), id);
        return new TenantDto(id, request.name().trim(), schema, "PENDING_SETUP");
    }

    @Transactional(readOnly = true)
    public List<TenantDto> listTenants() {
        return jdbc.query("SELECT id, name, schema_name, status FROM public.tenants ORDER BY name, id", TENANT);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN') or @tenantGuard.isMemberOf(authentication, #tenantId)")
    public TenantDetails tenant(UUID tenantId) {
        return new TenantDetails(findTenant(tenantId, false), modulesFor(tenantId));
    }

    @Transactional
    public ModuleDto createModule(CreateModuleRequest request) {
        String code = normalizeCode(request.code());
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM public.modules WHERE code = ?)",
                Boolean.class, code))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Module code already exists");
        }
        Long id = jdbc.queryForObject("INSERT INTO public.modules (name, code, description) VALUES (?, ?, ?) RETURNING id",
                Long.class, request.name().trim(), code, request.description().trim());
        return new ModuleDto(id, request.name().trim(), code, request.description().trim(), provisioning.supports(code));
    }

    @Transactional(readOnly = true)
    public List<ModuleDto> listModules() {
        return jdbc.query("SELECT id, name, code, description FROM public.modules ORDER BY code", moduleMapper());
    }

    @Transactional(readOnly = true)
    public List<ModuleDto> tenantModules(UUID tenantId) {
        findTenant(tenantId, false);
        return modulesFor(tenantId);
    }

    @Transactional
    public List<TenantDetails> assignModules(AssignModulesRequest request) {
        List<String> codes = request.moduleCodes().stream().map(PlatformService::normalizeCode).distinct().sorted().toList();
        for (String code : codes) {
            if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM public.modules WHERE code = ?)", Boolean.class, code))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown module: " + code);
            }
            if (!provisioning.supports(code)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No migrations for module: " + code);
            }
        }
        // Consistent lock order prevents deadlocks across overlapping bulk assignments.
        List<TenantDto> tenants = request.tenantIds().stream().distinct().sorted().map(id -> findTenant(id, true)).toList();
        for (TenantDto tenant : tenants) {
            provisioning.provision(tenant.schemaName(), codes);
            for (String code : codes) {
                jdbc.update("""
                        INSERT INTO public.tenant_modules (tenant_id, module_key) VALUES (?, ?)
                        ON CONFLICT (tenant_id, module_key) DO NOTHING
                        """, tenant.id(), code);
            }
        }
        return tenants.stream().map(t -> new TenantDetails(t, modulesFor(t.id()))).toList();
    }

    private TenantDto findTenant(UUID id, boolean lock) {
        List<TenantDto> tenants = jdbc.query("SELECT id, name, schema_name, status FROM public.tenants WHERE id = ?"
                + (lock ? " FOR UPDATE" : ""), TENANT, id);
        if (tenants.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
        return tenants.getFirst();
    }

    private List<ModuleDto> modulesFor(UUID id) {
        return jdbc.query("""
                SELECT m.id, m.name, m.code, m.description FROM public.modules m
                JOIN public.tenant_modules tm ON tm.module_key = m.code WHERE tm.tenant_id = ? ORDER BY m.code
                """, moduleMapper(), id);
    }

    private RowMapper<ModuleDto> moduleMapper() {
        return (rs, row) -> new ModuleDto(rs.getLong("id"), rs.getString("name"), rs.getString("code"),
                rs.getString("description"), provisioning.supports(rs.getString("code")));
    }

    private static String normalizeCode(String code) { return code.toUpperCase(Locale.ROOT); }
}
