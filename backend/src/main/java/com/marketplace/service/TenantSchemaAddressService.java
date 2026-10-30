package com.marketplace.service;

import com.marketplace.domain.RoleType;
import com.marketplace.domain.TenantEntity;
import com.marketplace.domain.UserRoleEntity;
import com.marketplace.repository.TenantRepository;
import com.marketplace.repository.UserRoleRepository;
import com.marketplace.web.dto.AddressDto;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Service
public class TenantSchemaAddressService {

    private static final String SCHEMA_PATTERN = "^tenant_[a-z0-9_]+$";

    private final JdbcTemplate jdbcTemplate;
    private final UserRoleRepository userRoleRepository;
    private final TenantRepository tenantRepository;

    public TenantSchemaAddressService(
            JdbcTemplate jdbcTemplate,
            UserRoleRepository userRoleRepository,
            TenantRepository tenantRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRoleRepository = userRoleRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<TenantEntity> tenantsForRole(Long userId, RoleType role) {
        return userRoleRepository.findByUserIdAndRole(userId, role).stream()
                .map(UserRoleEntity::getTenant)
                .filter(t -> t != null)
                .collect(LinkedHashMap<UUID, TenantEntity>::new, (map, t) -> map.putIfAbsent(t.getId(), t), LinkedHashMap::putAll)
                .values()
                .stream()
                .toList();
    }

    public List<AddressDto> listAllAddressesForTenantAdmin(Long userId) {
        List<AddressDto> result = new ArrayList<>();
        for (TenantEntity tenant : tenantsForRole(userId, RoleType.TENANT_ADMIN)) {
            result.addAll(listAddressesInSchema(tenant));
        }
        return result;
    }

    public List<AddressDto> listAssignedAddressesForStoreAdmin(Long userId) {
        List<AddressDto> result = new ArrayList<>();
        for (TenantEntity tenant : tenantsForRole(userId, RoleType.STORE_ADMIN)) {
            result.addAll(listAssignedAddressesInSchema(tenant, userId));
        }
        return result;
    }

    public List<AddressDto> listAddressesInSchema(TenantEntity tenant) {
        validateSchemaName(tenant.getSchemaName());
        String sql = """
                SELECT id, label, city, street_address
                FROM %s.addresses
                WHERE active = TRUE
                ORDER BY COALESCE(label, city), id
                """.formatted(tenant.getSchemaName());
        return jdbcTemplate.queryForList(sql).stream()
                .map(row -> toAddressDto(row, tenant))
                .toList();
    }

    private List<AddressDto> listAssignedAddressesInSchema(TenantEntity tenant, Long userId) {
        validateSchemaName(tenant.getSchemaName());
        String schema = tenant.getSchemaName();
        String sql = """
                SELECT a.id, a.label, a.city, a.street_address
                FROM %s.addresses a
                JOIN %s.address_admins aa ON aa.address_id = a.id
                WHERE aa.user_id = ? AND a.active = TRUE
                ORDER BY COALESCE(a.label, a.city), a.id
                """.formatted(schema, schema);
        return jdbcTemplate.queryForList(sql, userId).stream()
                .map(row -> toAddressDto(row, tenant))
                .toList();
    }

    public List<java.util.Map<String, Object>> listSharedBasketsInSchema(TenantEntity tenant) {
        if (jdbcTemplate.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class,
                tenant.getSchemaName() + ".baskets") != Boolean.TRUE) {
            return List.of();
        }
        validateSchemaName(tenant.getSchemaName());
        String sql = """
                SELECT id, title, shared, active
                FROM %s.baskets
                WHERE shared = TRUE AND active = TRUE
                ORDER BY title
                """.formatted(tenant.getSchemaName());
        return jdbcTemplate.queryForList(sql).stream()
                .map(row -> {
                    java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("id", row.get("id"));
                    map.put("title", row.get("title"));
                    map.put("tenantId", tenant.getId());
                    map.put("tenantName", tenant.getName());
                    return map;
                })
                .toList();
    }

    private AddressDto toAddressDto(java.util.Map<String, Object> row, TenantEntity tenant) {
        String label = (String) row.get("label");
        return new AddressDto(
                ((Number) row.get("id")).longValue(),
                label,
                (String) row.get("city"),
                (String) row.get("street_address"),
                tenant.getId(),
                tenant.getName()
        );
    }

    public void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches(SCHEMA_PATTERN)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid tenant schema");
        }
    }

    public boolean isTenantAdminFor(Long userId, UUID tenantId) {
        return tenantsForRole(userId, RoleType.TENANT_ADMIN).stream()
                .anyMatch(t -> t.getId().equals(tenantId));
    }

    public boolean canManageAddress(Long userId, UUID tenantId, Long addressId) {
        if (isTenantAdminFor(userId, tenantId)) {
            return true;
        }
        TenantEntity tenant = tenantsForRole(userId, RoleType.STORE_ADMIN).stream()
                .filter(t -> t.getId().equals(tenantId))
                .findFirst()
                .orElse(null);
        if (tenant == null) {
            return false;
        }
        validateSchemaName(tenant.getSchemaName());
        String sql = """
                SELECT 1 FROM %s.address_admins
                WHERE address_id = ? AND user_id = ?
                """.formatted(tenant.getSchemaName());
        return !jdbcTemplate.queryForList(sql, addressId, userId).isEmpty();
    }

    public boolean canManageBasket(Long userId, UUID tenantId, Long basketId) {
        String schema = schemaForTenant(tenantId);
        Long addressId = jdbcTemplate.queryForObject(
                "SELECT address_id FROM " + schema + ".baskets WHERE id = ?",
                Long.class,
                basketId
        );
        return canManageAddress(userId, tenantId, addressId);
    }

    public boolean canManageStudio(Long userId, UUID tenantId, Long studioId) {
        String schema = schemaForTenant(tenantId);
        Long addressId = jdbcTemplate.queryForObject(
                "SELECT address_id FROM " + schema + ".studios WHERE id = ?",
                Long.class,
                studioId
        );
        return canManageAddress(userId, tenantId, addressId);
    }

    public boolean canManageActivity(Long userId, UUID tenantId, Long activityId) {
        String schema = schemaForTenant(tenantId);
        Long addressId = jdbcTemplate.queryForObject(
                "SELECT address_id FROM " + schema + ".activities WHERE id = ?",
                Long.class,
                activityId
        );
        return canManageAddress(userId, tenantId, addressId);
    }

    private String schemaForTenant(UUID tenantId) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        validateSchemaName(tenant.getSchemaName());
        return tenant.getSchemaName();
    }

}
