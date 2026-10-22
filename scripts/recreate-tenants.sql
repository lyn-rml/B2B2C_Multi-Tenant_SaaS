-- =============================================================================
-- Student Marketplace — Tenant schema recreation (DDL + RLS)
--
-- Run as postgres superuser BEFORE sql/setup.sql:
--   psql "postgresql://postgres@127.0.0.1:5432/student_marketplace" \
--        -f scripts/recreate-tenants.sql
--
-- What this script does:
--   1. Drops the old tenant schemas (tenant_leclerc, tenant_entraide, tenant_campus)
--   2. Creates four new tenant schemas
--   3. Creates all tables with FKs, CHECKs, and indexes
--   4. Enables FORCE ROW LEVEL SECURITY on every tenant table
--   5. Creates RLS policies (matching app.has_tenant_access function)
--   6. Grants ALL privileges on tables+sequences to the app_rls role
--
-- Tenant UUIDs are fixed here and MUST match public.tenants rows in sql/setup.sql:
--   tenant_supermarket1:      aa000001-0000-4000-8000-000000000001
--   tenant_supermarket2:      aa000002-0000-4000-8000-000000000002
--   tenant_student_residence: aa000003-0000-4000-8000-000000000003
--   tenant_association:       aa000004-0000-4000-8000-000000000004
--
-- Design note: studios.process_id and activities.process_id are NULLABLE here
-- (unlike the original V1 schema) so rows can be created before a schedule exists.
-- =============================================================================


-- =============================================================================
-- 1. DROP OLD SCHEMAS
-- =============================================================================
DROP SCHEMA IF EXISTS tenant_leclerc     CASCADE;
DROP SCHEMA IF EXISTS tenant_entraide    CASCADE;
DROP SCHEMA IF EXISTS tenant_campus      CASCADE;


-- =============================================================================
-- 2. CREATE NEW SCHEMAS + GRANT USAGE
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS tenant_supermarket1;
CREATE SCHEMA IF NOT EXISTS tenant_supermarket2;
CREATE SCHEMA IF NOT EXISTS tenant_student_residence;
CREATE SCHEMA IF NOT EXISTS tenant_association;

GRANT USAGE ON SCHEMA tenant_supermarket1      TO app_rls;
GRANT USAGE ON SCHEMA tenant_supermarket2      TO app_rls;
GRANT USAGE ON SCHEMA tenant_student_residence TO app_rls;
GRANT USAGE ON SCHEMA tenant_association       TO app_rls;


-- =============================================================================
-- 3. TABLE DDL
-- =============================================================================

-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
-- tenant_supermarket1  (PRODUCTS_BASKETS + EMPLOYEE_PLANNING)
-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

CREATE TABLE tenant_supermarket1.addresses (
    id             BIGSERIAL PRIMARY KEY,
    label          VARCHAR   NOT NULL,
    street_address VARCHAR   NOT NULL,
    postal_code    VARCHAR   NOT NULL,
    city           VARCHAR   NOT NULL,
    country        VARCHAR   NOT NULL DEFAULT 'France',
    active         BOOL      NOT NULL DEFAULT TRUE
);

CREATE TABLE tenant_supermarket1.address_admins (
    address_id BIGINT NOT NULL REFERENCES tenant_supermarket1.addresses(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    PRIMARY KEY (address_id, user_id)
);

CREATE TABLE tenant_supermarket1.products (
    id         BIGSERIAL PRIMARY KEY,
    address_id BIGINT    NOT NULL REFERENCES tenant_supermarket1.addresses(id),
    name       VARCHAR   NOT NULL,
    active     BOOL      NOT NULL DEFAULT TRUE
);

CREATE TABLE tenant_supermarket1.product_batches (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT    NOT NULL REFERENCES tenant_supermarket1.products(id),
    expiry_date DATE      NOT NULL,
    quantity    NUMERIC   NOT NULL,
    active      BOOL      NOT NULL DEFAULT TRUE
);

CREATE INDEX product_batches_product_expiry_sm1
    ON tenant_supermarket1.product_batches (product_id, expiry_date);

CREATE TABLE tenant_supermarket1.baskets (
    id          BIGSERIAL PRIMARY KEY,
    address_id  BIGINT    NOT NULL REFERENCES tenant_supermarket1.addresses(id),
    process_id  BIGINT    REFERENCES public.process(id),
    title       VARCHAR   NOT NULL,
    description TEXT,
    price       NUMERIC,
    status      VARCHAR   NOT NULL DEFAULT 'NOT_SHARED'
                          CHECK (status IN ('SHARED', 'NOT_SHARED')),
    shared      BOOL      NOT NULL DEFAULT FALSE,
    active      BOOL      NOT NULL DEFAULT TRUE
);

CREATE INDEX baskets_shared_active_sm1
    ON tenant_supermarket1.baskets (shared, active);

CREATE TABLE tenant_supermarket1.basket_items (
    basket_id        BIGINT  NOT NULL REFERENCES tenant_supermarket1.baskets(id) ON DELETE CASCADE,
    product_batch_id BIGINT  NOT NULL REFERENCES tenant_supermarket1.product_batches(id),
    quantity         NUMERIC NOT NULL,
    PRIMARY KEY (basket_id, product_batch_id)
);

CREATE TABLE tenant_supermarket1.planning (
    id            BIGSERIAL PRIMARY KEY,
    address_id    BIGINT    NOT NULL REFERENCES tenant_supermarket1.addresses(id),
    user_id       BIGINT    NOT NULL REFERENCES public.users(id),
    plan_date     DATE      NOT NULL,
    disponibility JSONB     NOT NULL,
    active        BOOL      NOT NULL DEFAULT TRUE
);

CREATE TABLE tenant_supermarket1.employee_addresses (
    address_id BIGINT NOT NULL REFERENCES tenant_supermarket1.addresses(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    PRIMARY KEY (address_id, user_id)
);

-- RLS — tenant_supermarket1
ALTER TABLE tenant_supermarket1.addresses       ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.address_admins  ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.products        ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.product_batches ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.baskets         ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.basket_items    ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.planning        ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.employee_addresses ENABLE ROW LEVEL SECURITY;

ALTER TABLE tenant_supermarket1.addresses       FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.address_admins  FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.products        FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.product_batches FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.baskets         FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.basket_items    FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.planning        FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket1.employee_addresses FORCE ROW LEVEL SECURITY;

CREATE POLICY sm1_addresses_access ON tenant_supermarket1.addresses
    USING (app.has_tenant_access('aa000001-0000-4000-8000-000000000001'::uuid));

CREATE POLICY sm1_address_admins_access ON tenant_supermarket1.address_admins
    USING (app.has_tenant_access('aa000001-0000-4000-8000-000000000001'::uuid));

CREATE POLICY sm1_products_access ON tenant_supermarket1.products
    USING (app.has_tenant_access('aa000001-0000-4000-8000-000000000001'::uuid));

CREATE POLICY sm1_product_batches_access ON tenant_supermarket1.product_batches
    USING (app.has_tenant_access('aa000001-0000-4000-8000-000000000001'::uuid));

CREATE POLICY sm1_baskets_access ON tenant_supermarket1.baskets
    USING (
        app.has_tenant_access('aa000001-0000-4000-8000-000000000001'::uuid)
        OR (shared = TRUE AND active = TRUE
            AND current_setting('app.active_role', TRUE) = 'STUDENT')
    );

CREATE POLICY sm1_basket_items_access ON tenant_supermarket1.basket_items
    USING (app.has_tenant_access('aa000001-0000-4000-8000-000000000001'::uuid));

CREATE POLICY sm1_planning_access ON tenant_supermarket1.planning
    USING (app.has_tenant_access('aa000001-0000-4000-8000-000000000001'::uuid));

CREATE POLICY sm1_employee_addresses_access ON tenant_supermarket1.employee_addresses
    USING (app.has_tenant_access('aa000001-0000-4000-8000-000000000001'::uuid));

GRANT ALL ON ALL TABLES    IN SCHEMA tenant_supermarket1 TO app_rls;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA tenant_supermarket1 TO app_rls;


-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
-- tenant_supermarket2  (PRODUCTS_BASKETS + EMPLOYEE_PLANNING)
-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

CREATE TABLE tenant_supermarket2.addresses (
    id             BIGSERIAL PRIMARY KEY,
    label          VARCHAR   NOT NULL,
    street_address VARCHAR   NOT NULL,
    postal_code    VARCHAR   NOT NULL,
    city           VARCHAR   NOT NULL,
    country        VARCHAR   NOT NULL DEFAULT 'France',
    active         BOOL      NOT NULL DEFAULT TRUE
);

CREATE TABLE tenant_supermarket2.address_admins (
    address_id BIGINT NOT NULL REFERENCES tenant_supermarket2.addresses(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    PRIMARY KEY (address_id, user_id)
);

CREATE TABLE tenant_supermarket2.products (
    id         BIGSERIAL PRIMARY KEY,
    address_id BIGINT    NOT NULL REFERENCES tenant_supermarket2.addresses(id),
    name       VARCHAR   NOT NULL,
    active     BOOL      NOT NULL DEFAULT TRUE
);

CREATE TABLE tenant_supermarket2.product_batches (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT    NOT NULL REFERENCES tenant_supermarket2.products(id),
    expiry_date DATE      NOT NULL,
    quantity    NUMERIC   NOT NULL,
    active      BOOL      NOT NULL DEFAULT TRUE
);

CREATE INDEX product_batches_product_expiry_sm2
    ON tenant_supermarket2.product_batches (product_id, expiry_date);

CREATE TABLE tenant_supermarket2.baskets (
    id          BIGSERIAL PRIMARY KEY,
    address_id  BIGINT    NOT NULL REFERENCES tenant_supermarket2.addresses(id),
    process_id  BIGINT    REFERENCES public.process(id),
    title       VARCHAR   NOT NULL,
    description TEXT,
    price       NUMERIC,
    status      VARCHAR   NOT NULL DEFAULT 'NOT_SHARED'
                          CHECK (status IN ('SHARED', 'NOT_SHARED')),
    shared      BOOL      NOT NULL DEFAULT FALSE,
    active      BOOL      NOT NULL DEFAULT TRUE
);

CREATE INDEX baskets_shared_active_sm2
    ON tenant_supermarket2.baskets (shared, active);

CREATE TABLE tenant_supermarket2.basket_items (
    basket_id        BIGINT  NOT NULL REFERENCES tenant_supermarket2.baskets(id) ON DELETE CASCADE,
    product_batch_id BIGINT  NOT NULL REFERENCES tenant_supermarket2.product_batches(id),
    quantity         NUMERIC NOT NULL,
    PRIMARY KEY (basket_id, product_batch_id)
);

CREATE TABLE tenant_supermarket2.planning (
    id            BIGSERIAL PRIMARY KEY,
    address_id    BIGINT    NOT NULL REFERENCES tenant_supermarket2.addresses(id),
    user_id       BIGINT    NOT NULL REFERENCES public.users(id),
    plan_date     DATE      NOT NULL,
    disponibility JSONB     NOT NULL,
    active        BOOL      NOT NULL DEFAULT TRUE
);

CREATE TABLE tenant_supermarket2.employee_addresses (
    address_id BIGINT NOT NULL REFERENCES tenant_supermarket2.addresses(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    PRIMARY KEY (address_id, user_id)
);

-- RLS — tenant_supermarket2
ALTER TABLE tenant_supermarket2.addresses       ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.address_admins  ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.products        ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.product_batches ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.baskets         ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.basket_items    ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.planning        ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.employee_addresses ENABLE ROW LEVEL SECURITY;

ALTER TABLE tenant_supermarket2.addresses       FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.address_admins  FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.products        FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.product_batches FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.baskets         FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.basket_items    FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.planning        FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_supermarket2.employee_addresses FORCE ROW LEVEL SECURITY;

CREATE POLICY sm2_addresses_access ON tenant_supermarket2.addresses
    USING (app.has_tenant_access('aa000002-0000-4000-8000-000000000002'::uuid));

CREATE POLICY sm2_address_admins_access ON tenant_supermarket2.address_admins
    USING (app.has_tenant_access('aa000002-0000-4000-8000-000000000002'::uuid));

CREATE POLICY sm2_products_access ON tenant_supermarket2.products
    USING (app.has_tenant_access('aa000002-0000-4000-8000-000000000002'::uuid));

CREATE POLICY sm2_product_batches_access ON tenant_supermarket2.product_batches
    USING (app.has_tenant_access('aa000002-0000-4000-8000-000000000002'::uuid));

CREATE POLICY sm2_baskets_access ON tenant_supermarket2.baskets
    USING (
        app.has_tenant_access('aa000002-0000-4000-8000-000000000002'::uuid)
        OR (shared = TRUE AND active = TRUE
            AND current_setting('app.active_role', TRUE) = 'STUDENT')
    );

CREATE POLICY sm2_basket_items_access ON tenant_supermarket2.basket_items
    USING (app.has_tenant_access('aa000002-0000-4000-8000-000000000002'::uuid));

CREATE POLICY sm2_planning_access ON tenant_supermarket2.planning
    USING (app.has_tenant_access('aa000002-0000-4000-8000-000000000002'::uuid));

CREATE POLICY sm2_employee_addresses_access ON tenant_supermarket2.employee_addresses
    USING (app.has_tenant_access('aa000002-0000-4000-8000-000000000002'::uuid));

GRANT ALL ON ALL TABLES    IN SCHEMA tenant_supermarket2 TO app_rls;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA tenant_supermarket2 TO app_rls;


-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
-- tenant_student_residence  (STUDIOS)
-- process_id is NULLABLE — studios can be created before a schedule exists
-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

CREATE TABLE tenant_student_residence.addresses (
    id             BIGSERIAL PRIMARY KEY,
    label          VARCHAR   NOT NULL,
    street_address VARCHAR   NOT NULL,
    postal_code    VARCHAR   NOT NULL,
    city           VARCHAR   NOT NULL,
    country        VARCHAR   NOT NULL DEFAULT 'France',
    active         BOOL      NOT NULL DEFAULT TRUE
);

CREATE TABLE tenant_student_residence.address_admins (
    address_id BIGINT NOT NULL REFERENCES tenant_student_residence.addresses(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    PRIMARY KEY (address_id, user_id)
);

CREATE TABLE tenant_student_residence.studios (
    id           BIGSERIAL PRIMARY KEY,
    address_id   BIGINT    NOT NULL REFERENCES tenant_student_residence.addresses(id) ON DELETE CASCADE,
    process_id   BIGINT    REFERENCES public.process(id),           -- nullable
    label        VARCHAR   NOT NULL,
    surface_m2   NUMERIC,
    monthly_rent NUMERIC,
    status       VARCHAR   NOT NULL DEFAULT 'NOT_SHARED'
                           CHECK (status IN ('SHARED', 'NOT_SHARED')),
    shared       BOOL      NOT NULL DEFAULT FALSE,
    active       BOOL      NOT NULL DEFAULT TRUE
);

-- RLS — tenant_student_residence
ALTER TABLE tenant_student_residence.addresses      ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_student_residence.address_admins ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_student_residence.studios        ENABLE ROW LEVEL SECURITY;

ALTER TABLE tenant_student_residence.addresses      FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_student_residence.address_admins FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_student_residence.studios        FORCE ROW LEVEL SECURITY;

CREATE POLICY res_addresses_access ON tenant_student_residence.addresses
    USING (app.has_tenant_access('aa000003-0000-4000-8000-000000000003'::uuid));

CREATE POLICY res_address_admins_access ON tenant_student_residence.address_admins
    USING (app.has_tenant_access('aa000003-0000-4000-8000-000000000003'::uuid));

CREATE POLICY res_studios_access ON tenant_student_residence.studios
    USING (
        app.has_tenant_access('aa000003-0000-4000-8000-000000000003'::uuid)
        OR (shared = TRUE AND active = TRUE
            AND current_setting('app.active_role', TRUE) = 'STUDENT')
    );

GRANT ALL ON ALL TABLES    IN SCHEMA tenant_student_residence TO app_rls;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA tenant_student_residence TO app_rls;


-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
-- tenant_association  (ASSOCIATION_CONTENT)
-- process_id is NULLABLE — activities can be created before a schedule exists
-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

CREATE TABLE tenant_association.addresses (
    id             BIGSERIAL PRIMARY KEY,
    label          VARCHAR   NOT NULL,
    street_address VARCHAR   NOT NULL,
    postal_code    VARCHAR   NOT NULL,
    city           VARCHAR   NOT NULL,
    country        VARCHAR   NOT NULL DEFAULT 'France',
    active         BOOL      NOT NULL DEFAULT TRUE
);

CREATE TABLE tenant_association.address_admins (
    address_id BIGINT NOT NULL REFERENCES tenant_association.addresses(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    PRIMARY KEY (address_id, user_id)
);

CREATE TABLE tenant_association.activities (
    id          BIGSERIAL    PRIMARY KEY,
    address_id  BIGINT       NOT NULL REFERENCES tenant_association.addresses(id) ON DELETE CASCADE,
    process_id  BIGINT       REFERENCES public.process(id),         -- nullable
    title       VARCHAR      NOT NULL,
    description TEXT,
    starts_at   TIMESTAMPTZ  NOT NULL,
    ends_at     TIMESTAMPTZ  NOT NULL,
    status      VARCHAR      NOT NULL DEFAULT 'NOT_SHARED'
                             CHECK (status IN ('SHARED', 'NOT_SHARED')),
    shared      BOOL         NOT NULL DEFAULT FALSE,
    active      BOOL         NOT NULL DEFAULT TRUE
);

-- RLS — tenant_association
ALTER TABLE tenant_association.addresses      ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_association.address_admins ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_association.activities     ENABLE ROW LEVEL SECURITY;

ALTER TABLE tenant_association.addresses      FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_association.address_admins FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_association.activities     FORCE ROW LEVEL SECURITY;

CREATE POLICY asso_addresses_access ON tenant_association.addresses
    USING (app.has_tenant_access('aa000004-0000-4000-8000-000000000004'::uuid));

CREATE POLICY asso_address_admins_access ON tenant_association.address_admins
    USING (app.has_tenant_access('aa000004-0000-4000-8000-000000000004'::uuid));

CREATE POLICY asso_activities_access ON tenant_association.activities
    USING (
        app.has_tenant_access('aa000004-0000-4000-8000-000000000004'::uuid)
        OR (shared = TRUE AND active = TRUE
            AND current_setting('app.active_role', TRUE) = 'STUDENT')
    );

GRANT ALL ON ALL TABLES    IN SCHEMA tenant_association TO app_rls;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA tenant_association TO app_rls;


\echo ''
\echo '=== recreate-tenants.sql complete ==='
\echo 'New schemas: tenant_supermarket1, tenant_supermarket2, tenant_student_residence, tenant_association'
\echo 'Run sql/setup.sql next (as app_rls) to seed data.'
