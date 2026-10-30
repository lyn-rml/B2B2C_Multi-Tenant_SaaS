package com.marketplace.service;

import com.marketplace.auth.TenantContextHolder;
import com.marketplace.domain.ProcessEntity;
import com.marketplace.domain.TenantEntity;
import com.marketplace.domain.TenantModuleEntity;
import com.marketplace.domain.ModuleKey;
import com.marketplace.repository.TenantModuleRepository;
import com.marketplace.repository.TenantRepository;
import com.marketplace.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TenantCatalogService {

    private final JdbcTemplate jdbcTemplate;
    private final TenantRepository tenantRepository;
    private final TenantModuleRepository tenantModuleRepository;
    private final TenantSchemaAddressService tenantSchemaAddressService;
    private final ProcessService processService;

    public TenantCatalogService(
            JdbcTemplate jdbcTemplate,
            TenantRepository tenantRepository,
            TenantModuleRepository tenantModuleRepository,
            TenantSchemaAddressService tenantSchemaAddressService,
            ProcessService processService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantRepository = tenantRepository;
        this.tenantModuleRepository = tenantModuleRepository;
        this.tenantSchemaAddressService = tenantSchemaAddressService;
        this.processService = processService;
    }

    public List<ProductDto> listProducts(UUID tenantId, Long addressId) {
        String schema = schema(tenantId);
        String sql = """
                SELECT id, address_id, name, active FROM %s.products
                WHERE address_id = ? AND active = TRUE ORDER BY name
                """.formatted(schema);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ProductDto(
                rs.getLong("id"),
                rs.getLong("address_id"),
                rs.getString("name"),
                rs.getBoolean("active")
        ), addressId);
    }

    public ProductDto createProduct(UUID tenantId, Long addressId, CreateProductRequest request) {
        String schema = schema(tenantId);
        String sql = """
                INSERT INTO %s.products (address_id, name, active)
                VALUES (?, ?, TRUE) RETURNING id, address_id, name, active
                """.formatted(schema);
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new ProductDto(
                rs.getLong("id"),
                rs.getLong("address_id"),
                rs.getString("name"),
                rs.getBoolean("active")
        ), addressId, request.name());
    }

    public List<ProductBatchDto> listBatches(UUID tenantId, Long productId) {
        String schema = schema(tenantId);
        String sql = """
                SELECT id, product_id, expiry_date, quantity, active FROM %s.product_batches
                WHERE product_id = ? AND active = TRUE ORDER BY expiry_date
                """.formatted(schema);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ProductBatchDto(
                rs.getLong("id"),
                rs.getLong("product_id"),
                rs.getDate("expiry_date").toLocalDate(),
                rs.getBigDecimal("quantity"),
                rs.getBoolean("active")
        ), productId);
    }

    public ProductBatchDto createBatch(UUID tenantId, Long productId, CreateProductBatchRequest request) {
        String schema = schema(tenantId);
        String sql = """
                INSERT INTO %s.product_batches (product_id, expiry_date, quantity, active)
                VALUES (?, ?, ?, TRUE) RETURNING id, product_id, expiry_date, quantity, active
                """.formatted(schema);
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new ProductBatchDto(
                rs.getLong("id"),
                rs.getLong("product_id"),
                rs.getDate("expiry_date").toLocalDate(),
                rs.getBigDecimal("quantity"),
                rs.getBoolean("active")
        ), productId, request.expiryDate(), request.quantity());
    }

    public List<BasketDto> listBaskets(UUID tenantId, Long addressId) {
        String schema = schema(tenantId);
        String sql = """
                SELECT id, address_id, process_id, title, description, price, status, shared, active
                FROM %s.baskets WHERE address_id = ? AND active = TRUE ORDER BY id DESC
                """.formatted(schema);
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapBasket(rs), addressId);
    }

    public BasketDto createBasket(UUID tenantId, Long addressId, CreateBasketRequest request) {
        String schema = schema(tenantId);
        String sql = """
                INSERT INTO %s.baskets (address_id, title, description, price, status, shared, active)
                VALUES (?, ?, ?, ?, 'NOT_SHARED', FALSE, TRUE)
                RETURNING id, address_id, process_id, title, description, price, status, shared, active
                """.formatted(schema);
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapBasket(rs),
                addressId, request.title(), request.description(), request.price());
    }

    public void addBasketItem(UUID tenantId, Long basketId, AddBasketItemRequest request) {
        String schema = schema(tenantId);
        String sql = """
                INSERT INTO %s.basket_items (basket_id, product_batch_id, quantity)
                VALUES (?, ?, ?)
                ON CONFLICT (basket_id, product_batch_id) DO UPDATE SET quantity = EXCLUDED.quantity
                """.formatted(schema);
        jdbcTemplate.update(sql, basketId, request.productBatchId(), request.quantity());
    }

    public BasketDto publishBasket(UUID tenantId, Long basketId, PublishRequest request) {
        ProcessEntity process = processService.findOrCreateByDate(tenantId,
                request.publicDate().atStartOfDay().atOffset(ZoneOffset.UTC));
        String schema = schema(tenantId);
        String sql = """
                UPDATE %s.baskets
                SET process_id = ?, shared = FALSE, status = 'NOT_SHARED'
                WHERE id = ?
                RETURNING id, address_id, process_id, title, description, price, status, shared, active
                """.formatted(schema);
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapBasket(rs), process.getId(), basketId);
    }

    public List<EmployeeAssignmentDto> listEmployeesAtAddress(UUID tenantId, Long addressId) {
        String schema = schema(tenantId);
        if (!tableExists(schema, "employee_addresses")) {
            return List.of();
        }
        String sql = """
                SELECT u.id, u.email, u.full_name
                FROM %s.employee_addresses ea
                JOIN public.users u ON u.id = ea.user_id
                WHERE ea.address_id = ?
                ORDER BY u.full_name
                """.formatted(schema);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new EmployeeAssignmentDto(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("full_name")
        ), addressId);
    }

    public void assignEmployee(UUID tenantId, Long addressId, AssignEmployeeRequest request) {
        String schema = schema(tenantId);
        if (!tableExists(schema, "employee_addresses")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee assignment not enabled for this tenant");
        }
        String sql = """
                INSERT INTO %s.employee_addresses (address_id, user_id)
                VALUES (?, ?) ON CONFLICT DO NOTHING
                """.formatted(schema);
        jdbcTemplate.update(sql, addressId, request.userId());
    }

    public List<StudioDto> listStudios(UUID tenantId, Long addressId) {
        String schema = schema(tenantId);
        if (!tableExists(schema, "studios")) {
            return List.of();
        }
        String sql = """
                SELECT id, address_id, process_id, label, surface_m2, monthly_rent, status, shared, active
                FROM %s.studios WHERE address_id = ? AND active = TRUE ORDER BY label
                """.formatted(schema);
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapStudio(rs), addressId);
    }

    public StudioDto createStudio(UUID tenantId, Long addressId, CreateStudioRequest request) {
        String schema = schema(tenantId);
        String sql = """
                INSERT INTO %s.studios (address_id, label, surface_m2, monthly_rent, status, shared, active)
                VALUES (?, ?, ?, ?, 'NOT_SHARED', FALSE, TRUE)
                RETURNING id, address_id, process_id, label, surface_m2, monthly_rent, status, shared, active
                """.formatted(schema);
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapStudio(rs),
                addressId, request.label(), request.surfaceM2(), request.monthlyRent());
    }

    public StudioDto publishStudio(UUID tenantId, Long studioId, PublishRequest request) {
        ProcessEntity process = processService.findOrCreateByDate(tenantId,
                request.publicDate().atStartOfDay().atOffset(ZoneOffset.UTC));
        String schema = schema(tenantId);
        String sql = """
                UPDATE %s.studios SET process_id = ?, shared = FALSE, status = 'NOT_SHARED'
                WHERE id = ?
                RETURNING id, address_id, process_id, label, surface_m2, monthly_rent, status, shared, active
                """.formatted(schema);
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapStudio(rs), process.getId(), studioId);
    }

    public List<ActivityDto> listActivities(UUID tenantId, Long addressId) {
        String schema = schema(tenantId);
        if (!tableExists(schema, "activities")) {
            return List.of();
        }
        String sql = """
                SELECT id, address_id, process_id, text, image_path, status, shared, active
                FROM %s.activities WHERE address_id = ? AND active = TRUE ORDER BY id DESC
                """.formatted(schema);
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapActivity(rs), addressId);
    }

    public ActivityDto createActivity(UUID tenantId, Long addressId, CreateActivityRequest request) {
        String schema = schema(tenantId);
        String sql = """
                INSERT INTO %s.activities
                (address_id, text, image_path, status, shared, active)
                VALUES (?, ?, ?, 'NOT_SHARED', FALSE, TRUE)
                RETURNING id, address_id, process_id, text, image_path, status, shared, active
                """.formatted(schema);
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapActivity(rs),
                addressId, request.text(), request.imagePath());
    }

    public ActivityDto publishActivity(UUID tenantId, Long activityId, PublishRequest request) {
        ProcessEntity process = processService.findOrCreateByDate(tenantId,
                request.publicDate().atStartOfDay().atOffset(ZoneOffset.UTC));
        String schema = schema(tenantId);
        String sql = """
                UPDATE %s.activities SET process_id = ?, shared = FALSE, status = 'NOT_SHARED'
                WHERE id = ?
                RETURNING id, address_id, process_id, text, image_path, status, shared, active
                """.formatted(schema);
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapActivity(rs), process.getId(), activityId);
    }

    public List<StudentOfferDto> listStudentOffers(UUID tenantIdFilter, OfferType typeFilter) {
        TenantContextHolder.requireActiveRole(com.marketplace.domain.RoleType.STUDENT);
        List<StudentOfferDto> offers = new ArrayList<>();
        for (TenantEntity tenant : tenantRepository.findAll()) {
            if (tenantIdFilter != null && !tenant.getId().equals(tenantIdFilter)) {
                continue;
            }
            List<TenantModuleEntity> modules = tenantModuleRepository.findByTenant_IdOrderByModuleKeyAsc(tenant.getId());
            boolean hasBaskets = modules.stream().anyMatch(m -> m.getModuleKey() == ModuleKey.PRODUCTS_BASKETS);
            boolean hasStudios = modules.stream().anyMatch(m -> m.getModuleKey() == ModuleKey.STUDIOS);
            boolean hasActivities = modules.stream().anyMatch(m -> m.getModuleKey() == ModuleKey.ACTIVITIES);

            if ((typeFilter == null || typeFilter == OfferType.BASKET) && hasBaskets) {
                offers.addAll(sharedBaskets(tenant));
            }
            if ((typeFilter == null || typeFilter == OfferType.STUDIO) && hasStudios) {
                offers.addAll(sharedStudios(tenant));
            }
            if ((typeFilter == null || typeFilter == OfferType.ACTIVITY) && hasActivities) {
                offers.addAll(sharedActivities(tenant));
            }
        }
        return offers;
    }

    private List<StudentOfferDto> sharedBaskets(TenantEntity tenant) {
        String schema = tenant.getSchemaName();
        tenantSchemaAddressService.validateSchemaName(schema);
        if (!tableExists(schema, "baskets")) {
            return List.of();
        }
        String sql = """
                SELECT id, title, description, price FROM %s.baskets
                WHERE shared = TRUE AND active = TRUE ORDER BY title
                """.formatted(schema);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new StudentOfferDto(
                OfferType.BASKET,
                rs.getLong("id"),
                tenant.getId(),
                tenant.getName(),
                rs.getString("title"),
                rs.getString("description"),
                rs.getBigDecimal("price"),
                null,
                null
        ));
    }

    private List<StudentOfferDto> sharedStudios(TenantEntity tenant) {
        String schema = tenant.getSchemaName();
        tenantSchemaAddressService.validateSchemaName(schema);
        if (!tableExists(schema, "studios")) {
            return List.of();
        }
        String sql = """
                SELECT id, label, monthly_rent FROM %s.studios
                WHERE shared = TRUE AND active = TRUE ORDER BY label
                """.formatted(schema);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new StudentOfferDto(
                OfferType.STUDIO,
                rs.getLong("id"),
                tenant.getId(),
                tenant.getName(),
                rs.getString("label"),
                null,
                rs.getBigDecimal("monthly_rent"),
                null,
                null
        ));
    }

    private List<StudentOfferDto> sharedActivities(TenantEntity tenant) {
        String schema = tenant.getSchemaName();
        tenantSchemaAddressService.validateSchemaName(schema);
        if (!tableExists(schema, "activities")) {
            return List.of();
        }
        String sql = """
                SELECT id, text, image_path FROM %s.activities
                WHERE shared = TRUE AND active = TRUE ORDER BY id DESC
                """.formatted(schema);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new StudentOfferDto(
                OfferType.ACTIVITY,
                rs.getLong("id"),
                tenant.getId(),
                tenant.getName(),
                null,
                null,
                null,
                rs.getString("text"),
                rs.getString("image_path")
        ));
    }

    private boolean tableExists(String schema, String table) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = ? AND table_name = ?
                """,
                Integer.class,
                schema,
                table
        );
        return count != null && count > 0;
    }

    private String schema(UUID tenantId) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        tenantSchemaAddressService.validateSchemaName(tenant.getSchemaName());
        return tenant.getSchemaName();
    }

    private BasketDto mapBasket(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new BasketDto(
                rs.getLong("id"),
                rs.getLong("address_id"),
                rs.getObject("process_id", Long.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getBigDecimal("price"),
                rs.getString("status"),
                rs.getBoolean("shared"),
                rs.getBoolean("active")
        );
    }

    private StudioDto mapStudio(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new StudioDto(
                rs.getLong("id"),
                rs.getLong("address_id"),
                rs.getObject("process_id", Long.class),
                rs.getString("label"),
                rs.getBigDecimal("surface_m2"),
                rs.getBigDecimal("monthly_rent"),
                rs.getString("status"),
                rs.getBoolean("shared"),
                rs.getBoolean("active")
        );
    }

    private ActivityDto mapActivity(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ActivityDto(
                rs.getLong("id"), rs.getLong("address_id"),
                rs.getObject("process_id", Long.class),
                rs.getString("text"), rs.getString("image_path"),
                rs.getString("status"), rs.getBoolean("shared"), rs.getBoolean("active")
        );
    }
}
