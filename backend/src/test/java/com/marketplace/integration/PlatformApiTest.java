package com.marketplace.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "MARKETPLACE_TEST_JDBC_URL", matches = "jdbc:postgresql://[^/]+/marketplace_test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlatformApiTest {
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("MARKETPLACE_TEST_JDBC_URL"));
        properties.add("spring.datasource.username", () -> "marketplace_test_app");
        properties.add("spring.datasource.password", () -> "");
        properties.add("app.jwt.secret", () -> "isolated-integration-test-key-at-least-32-bytes-long");
        properties.add("logging.level.root", () -> "WARN");
    }

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.marketplace.service.MailService mail;
    String admin;

    @BeforeAll
    void startupDoesNotInsertData() {
        assertEquals("marketplace_test", jdbc.queryForObject("SELECT current_database()", String.class));
        for (String table : List.of("users", "tenants", "modules", "tenant_modules", "tenant_memberships", "user_roles", "process", "published_baskets", "reservations")) {
            assertEquals(0L, count("SELECT count(*) FROM public." + table), "Startup inserted data in " + table);
        }
        assertFalse(jdbc.queryForObject("SELECT rolsuper FROM pg_roles WHERE rolname = current_user", Boolean.class));
    }

    @BeforeEach
    void fixtures() {
        // This class is enabled only against the disposable database made by test-postgres.sh.
        assertEquals("marketplace_test", jdbc.queryForObject("SELECT current_database()", String.class));
        for (String schema : jdbc.queryForList("SELECT schema_name FROM public.tenants", String.class)) {
            assertTrue(schema.matches("tenant_[a-z0-9_]+"));
            jdbc.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
        }
        jdbc.execute("TRUNCATE public.users, public.tenants, public.modules CASCADE");
        admin = register("platform@example.test");
        jdbc.update("INSERT INTO public.user_roles (user_id, role) VALUES (?, 'PLATFORM_ADMIN')", userId("platform@example.test"));
    }

    @Test
    void registrationAndLoginRespectRolesAndAccountState() {
        String token = register("Student@Example.test");
        JsonNode me = ok(call(HttpMethod.GET, "/api/auth/me", token, null));
        assertEquals("student@example.test", me.get("email").asText());
        assertEquals("STUDENT", me.get("roles").get(0).get("role").asText());
        assertEquals(409, call(HttpMethod.POST, "/api/auth/register", null, registration("STUDENT@example.test", "STUDENT")).getStatusCode().value());
        for (String role : List.of("PLATFORM_ADMIN", "TENANT_ADMIN", "STORE_ADMIN", "EMPLOYEE")) {
            assertEquals(403, call(HttpMethod.POST, "/api/auth/register", null, registration(role + "@example.test", role)).getStatusCode().value());
        }
        assertEquals(401, call(HttpMethod.GET, "/api/platform/tenants", null, null).getStatusCode().value());
        assertEquals(403, call(HttpMethod.GET, "/api/platform/tenants", token, null).getStatusCode().value());
        assertEquals(401, call(HttpMethod.GET, "/api/auth/me", "invalid", null).getStatusCode().value());
        assertEquals(401, call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", "student@example.test", "password", "wrong")).getStatusCode().value());
        JsonNode login = ok(call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", "STUDENT@example.test", "password", "Test-password-123")));
        assertFalse(login.get("token").asText().isBlank());
        jdbc.update("UPDATE public.users SET is_active = false WHERE email = 'student@example.test'");
        assertEquals(401, call(HttpMethod.GET, "/api/auth/me", token, null).getStatusCode().value());
        jdbc.update("DELETE FROM public.user_roles WHERE role = 'PLATFORM_ADMIN'");
        assertEquals(403, call(HttpMethod.GET, "/api/platform/tenants", admin, null).getStatusCode().value());
    }

    @Test
    void createsTenantAtomicallyAndEnforcesMembershipForEveryStaffRole() {
        String owner = register("owner@example.test");
        String outsider = register("outsider@example.test");
        JsonNode tenant = createTenant("owner@example.test");
        String id = tenant.get("id").asText();
        assertEquals("PENDING_SETUP", tenant.get("status").asText());
        assertEquals(0L, count("SELECT count(*) FROM pg_namespace WHERE nspname = ?", tenant.get("schemaName").asText()));
        assertEquals(1L, count("SELECT count(*) FROM public.user_roles WHERE user_id = ? AND tenant_id = ? AND role = 'TENANT_ADMIN'", userId("owner@example.test"), UUID.fromString(id)));
        assertEquals(200, call(HttpMethod.GET, "/api/tenants/" + id, owner, null).getStatusCode().value());
        assertEquals(403, call(HttpMethod.GET, "/api/tenants/" + id, outsider, null).getStatusCode().value());
        HttpHeaders headers = headers(outsider);
        headers.set("X-Tenant-Id", id);
        headers.set("X-Active-Role", "TENANT_ADMIN");
        assertEquals(403, http.exchange("/api/tenants/" + id, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getStatusCode().value());
        assertEquals(403, call(HttpMethod.POST, "/api/platform/tenants", owner, Map.of("name", "Forbidden", "tenantAdminUserId", userId("outsider@example.test"))).getStatusCode().value());
        assertEquals(409, call(HttpMethod.POST, "/api/platform/tenants", admin, Map.of("name", "Duplicate admin", "tenantAdminUserId", userId("owner@example.test"))).getStatusCode().value());
        assertEquals(400, call(HttpMethod.POST, "/api/platform/tenants", admin, Map.of("name", "Missing admin", "tenantAdminUserId", Long.MAX_VALUE)).getStatusCode().value());
        assertEquals(1L, count("SELECT count(*) FROM public.tenants"));
        for (String role : List.of("STORE_ADMIN", "EMPLOYEE")) {
            jdbc.update("UPDATE public.user_roles SET role = ? WHERE tenant_id = ? AND user_id = ?", role, UUID.fromString(id), userId("owner@example.test"));
            assertEquals(200, call(HttpMethod.GET, "/api/tenants/" + id, owner, null).getStatusCode().value());
            JsonNode login = ok(call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", "owner@example.test", "password", "Test-password-123")));
            assertTrue(login.get("roles").toString().contains(role));
        }
        jdbc.update("DELETE FROM public.user_roles WHERE tenant_id = ?", UUID.fromString(id));
        assertEquals(403, call(HttpMethod.GET, "/api/tenants/" + id, owner, null).getStatusCode().value());
    }

    @Test
    void provisionsOnlySelectedModulesAndPreservesExistingData() {
        register("one@example.test"); register("two@example.test");
        JsonNode one = createTenant("one@example.test"); JsonNode two = createTenant("two@example.test");
        for (String code : List.of("STUDIOS", "ACTIVITIES", "PRODUCTS_BASKETS")) createModule(code);
        assertEquals(409, call(HttpMethod.POST, "/api/platform/modules", admin, Map.of("name", "duplicate", "code", "studios", "description", "duplicate")).getStatusCode().value());
        assertEquals(400, call(HttpMethod.POST, "/api/platform/modules", admin, Map.of("name", "bad", "code", "x; DROP SCHEMA public", "description", "bad")).getStatusCode().value());
        ok(assign(List.of(one, two), List.of("studios", "STUDIOS")));
        String s1 = one.get("schemaName").asText(); String s2 = two.get("schemaName").asText();
        Set<String> expected = Set.of("addresses", "address_admins", "employee_addresses", "schema_migrations", "studios");
        assertEquals(expected, tables(s1)); assertEquals(expected, tables(s2));
        jdbc.update("INSERT INTO " + s1 + ".addresses (label, street_address, postal_code, city) VALUES ('Only tenant one', 'Road', '75000', 'Paris')");
        assertEquals(0L, count("SELECT count(*) FROM " + s2 + ".addresses"));
        ok(assign(List.of(one), List.of("STUDIOS", "ACTIVITIES", "PRODUCTS_BASKETS")));
        ok(assign(List.of(one), List.of("STUDIOS", "ACTIVITIES")));
        assertEquals(1L, count("SELECT count(*) FROM " + s1 + ".addresses"));
        assertEquals(4L, count("SELECT count(*) FROM " + s1 + ".schema_migrations"));
        assertEquals(3L, count("SELECT count(*) FROM public.tenant_modules WHERE tenant_id = ?", UUID.fromString(one.get("id").asText())));
        assertTrue(tables(s1).containsAll(Set.of("products", "product_batches", "baskets", "basket_items", "activities")));
        assertEquals(expected, tables(s2));
        assertEquals(0L, count("SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = ? AND (c.relrowsecurity OR c.relforcerowsecurity)", s1));
        assertEquals(2, ok(call(HttpMethod.GET, "/api/platform/tenants", admin, null)).size());
        assertEquals(3, ok(call(HttpMethod.GET, "/api/platform/tenants/" + one.get("id").asText() + "/modules", admin, null)).size());
        assertEquals(0L, count("SELECT count(*) FROM " + s1 + ".activities"));
    }

    @Test
    void rejectsUnsupportedUnknownAndUnauthorizedAssignmentsWithoutCreatingSchema() {
        String owner = register("owner@example.test"); JsonNode tenant = createTenant("owner@example.test");
        JsonNode custom = createModule("CUSTOM_MODULE"); assertFalse(custom.get("provisioningSupported").asBoolean());
        assertEquals(409, assign(List.of(tenant), List.of("CUSTOM_MODULE")).getStatusCode().value());
        assertEquals(404, assign(List.of(tenant), List.of("MISSING")).getStatusCode().value());
        createModule("ACTIVITIES");
        assertEquals(403, call(HttpMethod.POST, "/api/platform/module-assignments", owner,
                Map.of("tenantIds", List.of(tenant.get("id").asText()), "moduleCodes", List.of("ACTIVITIES"))).getStatusCode().value());
        assertEquals(400, call(HttpMethod.POST, "/api/platform/module-assignments", admin,
                Map.of("tenantIds", List.of(), "moduleCodes", List.of("ACTIVITIES"))).getStatusCode().value());
        assertEquals(404, call(HttpMethod.POST, "/api/platform/module-assignments", admin,
                Map.of("tenantIds", List.of(tenant.get("id").asText(), UUID.randomUUID().toString()), "moduleCodes", List.of("ACTIVITIES"))).getStatusCode().value());
        assertEquals(0L, count("SELECT count(*) FROM public.tenant_modules"));
        assertEquals(0L, count("SELECT count(*) FROM pg_namespace WHERE nspname = ?", tenant.get("schemaName").asText()));
    }

    @Test
    void migrationFailureRollsBackEntireBulkAssignment() {
        register("one@example.test"); register("two@example.test");
        JsonNode one = createTenant("one@example.test"); JsonNode two = createTenant("two@example.test");
        createModule("STUDIOS");
        List<JsonNode> tenants = new ArrayList<>(List.of(one, two));
        tenants.sort(Comparator.comparing(t -> UUID.fromString(t.get("id").asText())));
        String untouched = tenants.getFirst().get("schemaName").asText();
        String conflict = tenants.getLast().get("schemaName").asText();
        jdbc.execute("CREATE SCHEMA " + conflict);
        jdbc.execute("CREATE TABLE " + conflict + ".studios (legacy_value TEXT)");
        jdbc.update("INSERT INTO " + conflict + ".studios VALUES ('keep me')");
        assertEquals(409, assign(tenants, List.of("STUDIOS")).getStatusCode().value());
        assertEquals(0L, count("SELECT count(*) FROM public.tenant_modules"));
        assertEquals(0L, count("SELECT count(*) FROM pg_namespace WHERE nspname = ?", untouched));
        assertEquals(Set.of("studios"), tables(conflict));
        assertEquals("keep me", jdbc.queryForObject("SELECT legacy_value FROM " + conflict + ".studios", String.class));
    }

    @Test
    void concurrentAssignmentsAreIdempotentAndChecksumsAreEnforced() throws Exception {
        register("owner@example.test"); JsonNode tenant = createTenant("owner@example.test");
        createModule("ACTIVITIES");
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Integer> action = () -> { start.await(); return assign(List.of(tenant), List.of("ACTIVITIES")).getStatusCode().value(); };
            Future<Integer> a = pool.submit(action); Future<Integer> b = pool.submit(action); start.countDown();
            assertEquals(200, a.get(20, TimeUnit.SECONDS)); assertEquals(200, b.get(20, TimeUnit.SECONDS));
        }
        String schema = tenant.get("schemaName").asText();
        assertEquals(1L, count("SELECT count(*) FROM public.tenant_modules"));
        assertEquals(2L, count("SELECT count(*) FROM " + schema + ".schema_migrations"));
        jdbc.update("UPDATE " + schema + ".schema_migrations SET checksum = 'changed' WHERE module_code = 'ACTIVITIES'");
        assertEquals(409, assign(List.of(tenant), List.of("ACTIVITIES")).getStatusCode().value());
        assertEquals(1L, count("SELECT count(*) FROM public.tenant_modules"));
    }

    @Test
    void concurrentTenantCreationAssignsAdministratorOnlyOnce() throws Exception {
        register("owner@example.test");
        long ownerId = userId("owner@example.test");
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Integer> action = () -> {
                start.await();
                return call(HttpMethod.POST, "/api/platform/tenants", admin,
                        Map.of("name", "Concurrent tenant", "tenantAdminUserId", ownerId)).getStatusCode().value();
            };
            Future<Integer> a = pool.submit(action); Future<Integer> b = pool.submit(action); start.countDown();
            assertEquals(Set.of(201, 409), Set.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)));
        }
        assertEquals(1L, count("SELECT count(*) FROM public.tenants"));
        assertEquals(1L, count("SELECT count(*) FROM public.tenant_memberships"));
        assertEquals(1L, count("SELECT count(*) FROM public.user_roles WHERE role = 'TENANT_ADMIN'"));
    }

    @Test
    void otpStillAuthenticatesAndCannotBeReused() {
        register("otp@example.test");
        assertEquals(200, call(HttpMethod.POST, "/api/auth/otp/request", null, Map.of("email", "OTP@example.test")).getStatusCode().value());
        var code = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(mail).sendOtpCode(org.mockito.ArgumentMatchers.eq("otp@example.test"), code.capture());
        JsonNode result = ok(call(HttpMethod.POST, "/api/auth/otp/verify", null, Map.of("email", "otp@example.test", "code", code.getValue())));
        assertEquals(200, call(HttpMethod.GET, "/api/auth/me", result.get("token").asText(), null).getStatusCode().value());
        assertEquals(401, call(HttpMethod.POST, "/api/auth/otp/verify", null, Map.of("email", "otp@example.test", "code", code.getValue())).getStatusCode().value());
    }

    @Test
    void openApiContainsOnlyTheNewScopeAndExportsSnapshots() throws Exception {
        JsonNode spec = ok(call(HttpMethod.GET, "/v3/api-docs", null, null));
        Set<String> actual = new HashSet<>(); spec.get("paths").fieldNames().forEachRemaining(actual::add);
        assertEquals(Set.of("/api/auth/register", "/api/auth/login", "/api/auth/me", "/api/auth/otp/request", "/api/auth/otp/verify",
                "/api/health", "/api/platform/tenants", "/api/platform/tenants/{id}", "/api/platform/modules",
                "/api/platform/tenants/{id}/modules", "/api/platform/module-assignments", "/api/tenants/{id}"), actual);
        ((com.fasterxml.jackson.databind.node.ObjectNode) spec).remove("servers");
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/openapi.json"), json.writerWithDefaultPrettyPrinter().writeValueAsString(spec) + "\n");
        Files.writeString(Path.of("target/openapi.yaml"), new com.fasterxml.jackson.dataformat.yaml.YAMLMapper().writeValueAsString(spec));
        assertTrue(spec.get("paths").get("/api/auth/login").get("post").get("security").isEmpty());
        assertEquals(403, call(HttpMethod.GET, "/api/student/offers", admin, null).getStatusCode().value());
    }

    private String register(String email) {
        JsonNode result = ok(call(HttpMethod.POST, "/api/auth/register", null, registration(email, "STUDENT")));
        assertEquals("STUDENT", result.get("roles").get(0).get("role").asText());
        return result.get("token").asText();
    }
    private Map<String, Object> registration(String email, String role) {
        return Map.of("email", email, "fullName", "Test User", "password", "Test-password-123", "role", role);
    }
    private long userId(String email) { return jdbc.queryForObject("SELECT id FROM public.users WHERE email = ?", Long.class, email.toLowerCase(Locale.ROOT)); }
    private JsonNode createTenant(String email) {
        var response = call(HttpMethod.POST, "/api/platform/tenants", admin, Map.of("name", "Tenant " + email, "tenantAdminUserId", userId(email)));
        assertEquals(201, response.getStatusCode().value(), response.toString()); return response.getBody();
    }
    private JsonNode createModule(String code) {
        var response = call(HttpMethod.POST, "/api/platform/modules", admin, Map.of("name", code, "code", code, "description", "Module " + code));
        assertEquals(201, response.getStatusCode().value(), response.toString()); return response.getBody();
    }
    private ResponseEntity<JsonNode> assign(List<JsonNode> tenants, List<String> codes) {
        return call(HttpMethod.POST, "/api/platform/module-assignments", admin,
                Map.of("tenantIds", tenants.stream().map(t -> t.get("id").asText()).toList(), "moduleCodes", codes));
    }
    private Set<String> tables(String schema) { return new HashSet<>(jdbc.queryForList("SELECT tablename FROM pg_tables WHERE schemaname = ?", String.class, schema)); }
    private long count(String sql, Object... args) { return jdbc.queryForObject(sql, Long.class, args); }
    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token); return headers;
    }
    private ResponseEntity<JsonNode> call(HttpMethod method, String path, String token, Object body) {
        return http.exchange(path, method, new HttpEntity<>(body, headers(token)), JsonNode.class);
    }
    private JsonNode ok(ResponseEntity<JsonNode> response) {
        assertTrue(response.getStatusCode().is2xxSuccessful(), response.toString()); return response.getBody();
    }
}
