-- =============================================================================
-- Student Marketplace — V2 full seed  (DML only, re-runnable)
--
-- Prerequisites: run scripts/recreate-tenants.sql as postgres superuser first.
--
-- Run as app_rls:
--   psql "postgresql://app_rls:app_rls_dev_change_me@127.0.0.1:5432/student_marketplace" \
--        -f sql/setup.sql
--
-- Sections:
--   1. RESET          wipe all data (leaf → root FK order)
--   2. PUBLIC SEED    users · tenants · modules · memberships · roles
--   3. TENANT SEED    per-tenant data (addresses, products, baskets, studios, activities, planning)
--   4. VERIFY         row counts + structural checks
-- =============================================================================


-- =============================================================================
-- 1. RESET  (delete leaf tables first; set RLS context per tenant for tenant rows)
-- =============================================================================

-- ── Supermarket 1 ─────────────────────────────────────────────────────────────
BEGIN;
SELECT set_config('app.user_id',
    COALESCE((SELECT id::text FROM public.users WHERE email = 'admin1@sm1.test'), ''),
    false);
SELECT set_config('app.active_role', 'TENANT_ADMIN', false);
DELETE FROM tenant_supermarket1.basket_items;
DELETE FROM tenant_supermarket1.employee_addresses;
DELETE FROM tenant_supermarket1.planning;
DELETE FROM tenant_supermarket1.baskets;
DELETE FROM tenant_supermarket1.product_batches;
DELETE FROM tenant_supermarket1.products;
DELETE FROM tenant_supermarket1.address_admins;
DELETE FROM tenant_supermarket1.addresses;
COMMIT;

-- ── Supermarket 2 ─────────────────────────────────────────────────────────────
BEGIN;
SELECT set_config('app.user_id',
    COALESCE((SELECT id::text FROM public.users WHERE email = 'admin2@sm2.test'), ''),
    false);
SELECT set_config('app.active_role', 'TENANT_ADMIN', false);
DELETE FROM tenant_supermarket2.basket_items;
DELETE FROM tenant_supermarket2.employee_addresses;
DELETE FROM tenant_supermarket2.planning;
DELETE FROM tenant_supermarket2.baskets;
DELETE FROM tenant_supermarket2.product_batches;
DELETE FROM tenant_supermarket2.products;
DELETE FROM tenant_supermarket2.address_admins;
DELETE FROM tenant_supermarket2.addresses;
COMMIT;

-- ── Student Residence ─────────────────────────────────────────────────────────
BEGIN;
SELECT set_config('app.user_id',
    COALESCE((SELECT id::text FROM public.users WHERE email = 'admin@residence.test'), ''),
    false);
SELECT set_config('app.active_role', 'TENANT_ADMIN', false);
DELETE FROM tenant_student_residence.studios;
DELETE FROM tenant_student_residence.address_admins;
DELETE FROM tenant_student_residence.addresses;
COMMIT;

-- ── Association ───────────────────────────────────────────────────────────────
BEGIN;
SELECT set_config('app.user_id',
    COALESCE((SELECT id::text FROM public.users WHERE email = 'admin@asso.test'), ''),
    false);
SELECT set_config('app.active_role', 'TENANT_ADMIN', false);
DELETE FROM tenant_association.activities;
DELETE FROM tenant_association.address_admins;
DELETE FROM tenant_association.addresses;
COMMIT;

-- ── Public tables (no RLS) ────────────────────────────────────────────────────
BEGIN;
DELETE FROM public.user_roles;
DELETE FROM public.tenant_memberships;
DELETE FROM public.tenant_modules;
DELETE FROM public.process;
DELETE FROM public.tenants;
DELETE FROM public.users;
COMMIT;


-- =============================================================================
-- 2. PUBLIC SEED
-- =============================================================================
BEGIN;

-- ── Users (password = Password123!) ──────────────────────────────────────────
-- bcrypt hash for Password123!
INSERT INTO public.users (email, full_name, password_hash, is_active) VALUES
    -- Supermarket 1
    ('admin1@sm1.test',       'Admin Supermarché 1',    '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true),
    ('storeadmin1@sm1.test',  'Store Admin SM1',         '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true),
    ('emp1a@sm1.test',        'Employé A SM1',           '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true),
    ('emp1b@sm1.test',        'Employé B SM1',           '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true),
    -- Supermarket 2
    ('admin2@sm2.test',       'Admin Supermarché 2',    '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true),
    ('storeadmin2@sm2.test',  'Store Admin SM2',         '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true),
    ('emp2a@sm2.test',        'Employé A SM2',           '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true),
    ('emp2b@sm2.test',        'Employé B SM2',           '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true),
    -- Student Residence
    ('admin@residence.test',    'Admin Résidence',        '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true),
    ('addradmin@residence.test','Gestionnaire Résidence', '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true),
    -- Association
    ('admin@asso.test',         'Admin Association',      '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true),
    ('addradmin@asso.test',     'Responsable Asso',       '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true),
    ('member1@asso.test',       'Membre Association 1',   '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true),
    ('member2@asso.test',       'Membre Association 2',   '$2a$10$In1BTbrZH4pBxBZHLhcKwOTZPvXpHHp6bOgVMTFzEIT5wkaNNVGmi', true);

-- ── Tenants ───────────────────────────────────────────────────────────────────
-- UUIDs are fixed and match the RLS policies in scripts/recreate-tenants.sql
INSERT INTO public.tenants (id, name, schema_name, tenant_type) VALUES
    ('aa000001-0000-4000-8000-000000000001', 'Supermarket 1',     'tenant_supermarket1',      'SUPERMARKET'),
    ('aa000002-0000-4000-8000-000000000002', 'Supermarket 2',     'tenant_supermarket2',      'SUPERMARKET'),
    ('aa000003-0000-4000-8000-000000000003', 'Student Residence', 'tenant_student_residence', 'RESIDENCE'),
    ('aa000004-0000-4000-8000-000000000004', 'Association',       'tenant_association',       'ASSOCIATION');

-- ── Modules ───────────────────────────────────────────────────────────────────
INSERT INTO public.tenant_modules (tenant_id, module_key) VALUES
    ('aa000001-0000-4000-8000-000000000001', 'PRODUCTS_BASKETS'),
    ('aa000001-0000-4000-8000-000000000001', 'EMPLOYEE_PLANNING'),
    ('aa000002-0000-4000-8000-000000000002', 'PRODUCTS_BASKETS'),
    ('aa000002-0000-4000-8000-000000000002', 'EMPLOYEE_PLANNING'),
    ('aa000003-0000-4000-8000-000000000003', 'STUDIOS'),
    ('aa000004-0000-4000-8000-000000000004', 'ASSOCIATION_CONTENT');

-- ── Memberships ───────────────────────────────────────────────────────────────
INSERT INTO public.tenant_memberships (user_id, tenant_id)
SELECT u.id, t.id
FROM public.users u
JOIN public.tenants t ON TRUE
WHERE
    -- SM1 members
    (u.email IN ('admin1@sm1.test','storeadmin1@sm1.test','emp1a@sm1.test','emp1b@sm1.test')
     AND t.schema_name = 'tenant_supermarket1')
    -- SM2 members
 OR (u.email IN ('admin2@sm2.test','storeadmin2@sm2.test','emp2a@sm2.test','emp2b@sm2.test')
     AND t.schema_name = 'tenant_supermarket2')
    -- Residence members
 OR (u.email IN ('admin@residence.test','addradmin@residence.test')
     AND t.schema_name = 'tenant_student_residence')
    -- Association members
 OR (u.email IN ('admin@asso.test','addradmin@asso.test','member1@asso.test','member2@asso.test')
     AND t.schema_name = 'tenant_association');

-- ── Roles ─────────────────────────────────────────────────────────────────────

-- TENANT_ADMIN (one per user constraint enforced by partial unique index)
INSERT INTO public.user_roles (user_id, tenant_id, role)
SELECT u.id, t.id, 'TENANT_ADMIN'
FROM public.users u
JOIN public.tenants t ON TRUE
WHERE (u.email = 'admin1@sm1.test'      AND t.schema_name = 'tenant_supermarket1')
   OR (u.email = 'admin2@sm2.test'      AND t.schema_name = 'tenant_supermarket2')
   OR (u.email = 'admin@residence.test' AND t.schema_name = 'tenant_student_residence')
   OR (u.email = 'admin@asso.test'      AND t.schema_name = 'tenant_association');

-- STORE_ADMIN
INSERT INTO public.user_roles (user_id, tenant_id, role)
SELECT u.id, t.id, 'STORE_ADMIN'
FROM public.users u
JOIN public.tenants t ON TRUE
WHERE (u.email = 'storeadmin1@sm1.test' AND t.schema_name = 'tenant_supermarket1')
   OR (u.email = 'storeadmin2@sm2.test' AND t.schema_name = 'tenant_supermarket2')
   OR (u.email = 'addradmin@residence.test' AND t.schema_name = 'tenant_student_residence')
   OR (u.email = 'addradmin@asso.test'  AND t.schema_name = 'tenant_association');

-- EMPLOYEE (tenant-scoped)
INSERT INTO public.user_roles (user_id, tenant_id, role)
SELECT u.id, t.id, 'EMPLOYEE'
FROM public.users u
JOIN public.tenants t ON TRUE
WHERE (u.email = 'emp1a@sm1.test'   AND t.schema_name = 'tenant_supermarket1')
   OR (u.email = 'emp1b@sm1.test'   AND t.schema_name = 'tenant_supermarket1')
   OR (u.email = 'emp2a@sm2.test'   AND t.schema_name = 'tenant_supermarket2')
   OR (u.email = 'emp2b@sm2.test'   AND t.schema_name = 'tenant_supermarket2')
   OR (u.email = 'member1@asso.test' AND t.schema_name = 'tenant_association')
   OR (u.email = 'member2@asso.test' AND t.schema_name = 'tenant_association');

-- STUDENT (platform-wide, tenant_id = NULL)
-- emp1b and emp2b are also students (dual role persona)
INSERT INTO public.user_roles (user_id, tenant_id, role)
SELECT u.id, NULL, 'STUDENT'
FROM public.users u
WHERE u.email IN ('emp1b@sm1.test', 'emp2b@sm2.test');

COMMIT;


-- =============================================================================
-- 3. TENANT SEED
-- =============================================================================

-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
-- 3a. Supermarket 1  (Rennes + Paris)
-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
BEGIN;
SELECT set_config('app.user_id',
    (SELECT id::text FROM public.users WHERE email = 'admin1@sm1.test'),
    false);
SELECT set_config('app.active_role', 'TENANT_ADMIN', false);

-- Addresses
INSERT INTO tenant_supermarket1.addresses (label, street_address, postal_code, city) VALUES
    ('SM1 Rennes', '10 Rue de la Liberté',  '35000', 'Rennes'),
    ('SM1 Paris',  '25 Bd Haussmann',        '75009', 'Paris');

-- address_admins: storeadmin1 manages both SM1 addresses
INSERT INTO tenant_supermarket1.address_admins (address_id, user_id)
SELECT a.id, u.id
FROM tenant_supermarket1.addresses a
CROSS JOIN public.users u
WHERE u.email = 'storeadmin1@sm1.test';

INSERT INTO tenant_supermarket1.employee_addresses (address_id, user_id)
SELECT a.id, u.id
FROM tenant_supermarket1.addresses a
CROSS JOIN public.users u
WHERE (a.label = 'SM1 Rennes' AND u.email = 'emp1a@sm1.test')
   OR (a.label = 'SM1 Paris'  AND u.email = 'emp1b@sm1.test');

-- Products at Rennes
INSERT INTO tenant_supermarket1.products (address_id, name)
SELECT a.id, v.name
FROM tenant_supermarket1.addresses a
CROSS JOIN (VALUES ('Pain complet'), ('Yaourt bio'), ('Jus de pomme')) AS v(name)
WHERE a.label = 'SM1 Rennes';

-- Products at Paris
INSERT INTO tenant_supermarket1.products (address_id, name)
SELECT a.id, v.name
FROM tenant_supermarket1.addresses a
CROSS JOIN (VALUES ('Fromage râpé'), ('Salade mélangée')) AS v(name)
WHERE a.label = 'SM1 Paris';

-- Product batches (various expiry dates)
INSERT INTO tenant_supermarket1.product_batches (product_id, expiry_date, quantity)
SELECT p.id, v.exp::DATE, v.qty::NUMERIC
FROM tenant_supermarket1.products p
JOIN (VALUES
    ('Pain complet',    CURRENT_DATE + 1,  20),
    ('Pain complet',    CURRENT_DATE + 3,  15),
    ('Yaourt bio',      CURRENT_DATE + 2,  30),
    ('Jus de pomme',    CURRENT_DATE + 5,  12),
    ('Fromage râpé',    CURRENT_DATE + 4,   8),
    ('Salade mélangée', CURRENT_DATE + 1,  25)
) AS v(pname, exp, qty) ON p.name = v.pname;

-- Empty baskets (no basket_items, no process_id — to be configured later)
INSERT INTO tenant_supermarket1.baskets
    (address_id, title, description, price, status, shared, active)
SELECT a.id,
    'Panier anti-gaspi ' || a.city,
    'À configurer — produits proches de la DLC',
    NULL, 'NOT_SHARED', FALSE, TRUE
FROM tenant_supermarket1.addresses a;

-- Planning: emp1a works at Rennes (3 days), emp1b works at Paris (3 days)
INSERT INTO tenant_supermarket1.planning
    (address_id, user_id, plan_date, disponibility)
SELECT a.id, u.id, d.plan_date,
    '[{"start":"09:00","end":"12:00"},{"start":"14:00","end":"18:00"}]'::jsonb
FROM tenant_supermarket1.addresses a
CROSS JOIN public.users u
CROSS JOIN (
    SELECT CURRENT_DATE + 1 AS plan_date
    UNION ALL SELECT CURRENT_DATE + 2
    UNION ALL SELECT CURRENT_DATE + 3
) d
WHERE (a.label = 'SM1 Rennes' AND u.email = 'emp1a@sm1.test')
   OR (a.label = 'SM1 Paris'  AND u.email = 'emp1b@sm1.test');

COMMIT;


-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
-- 3b. Supermarket 2  (Paris + Brest)
-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
BEGIN;
SELECT set_config('app.user_id',
    (SELECT id::text FROM public.users WHERE email = 'admin2@sm2.test'),
    false);
SELECT set_config('app.active_role', 'TENANT_ADMIN', false);

-- Addresses
INSERT INTO tenant_supermarket2.addresses (label, street_address, postal_code, city) VALUES
    ('SM2 Paris', '42 Rue de Rivoli', '75001', 'Paris'),
    ('SM2 Brest', '8 Rue de Siam',    '29200', 'Brest');

-- address_admins: storeadmin2 manages both SM2 addresses
INSERT INTO tenant_supermarket2.address_admins (address_id, user_id)
SELECT a.id, u.id
FROM tenant_supermarket2.addresses a
CROSS JOIN public.users u
WHERE u.email = 'storeadmin2@sm2.test';

INSERT INTO tenant_supermarket2.employee_addresses (address_id, user_id)
SELECT a.id, u.id
FROM tenant_supermarket2.addresses a
CROSS JOIN public.users u
WHERE (a.label = 'SM2 Paris' AND u.email = 'emp2a@sm2.test')
   OR (a.label = 'SM2 Brest' AND u.email = 'emp2b@sm2.test');

-- Products at Paris
INSERT INTO tenant_supermarket2.products (address_id, name)
SELECT a.id, v.name
FROM tenant_supermarket2.addresses a
CROSS JOIN (VALUES ('Croissants'), ('Pain au chocolat')) AS v(name)
WHERE a.label = 'SM2 Paris';

-- Products at Brest
INSERT INTO tenant_supermarket2.products (address_id, name)
SELECT a.id, v.name
FROM tenant_supermarket2.addresses a
CROSS JOIN (VALUES ('Pâtes rigatoni'), ('Sauce bolognaise'), ('Baguette tradition')) AS v(name)
WHERE a.label = 'SM2 Brest';

-- Product batches
INSERT INTO tenant_supermarket2.product_batches (product_id, expiry_date, quantity)
SELECT p.id, v.exp::DATE, v.qty::NUMERIC
FROM tenant_supermarket2.products p
JOIN (VALUES
    ('Croissants',        CURRENT_DATE + 1,  48),
    ('Pain au chocolat',  CURRENT_DATE + 1,  36),
    ('Pâtes rigatoni',    CURRENT_DATE + 30, 60),
    ('Sauce bolognaise',  CURRENT_DATE + 20, 24),
    ('Baguette tradition',CURRENT_DATE + 1,  80)
) AS v(pname, exp, qty) ON p.name = v.pname;

-- Empty baskets
INSERT INTO tenant_supermarket2.baskets
    (address_id, title, description, price, status, shared, active)
SELECT a.id,
    'Panier anti-gaspi ' || a.city,
    'À configurer — produits proches de la DLC',
    NULL, 'NOT_SHARED', FALSE, TRUE
FROM tenant_supermarket2.addresses a;

-- Planning: emp2a works at Paris, emp2b works at Brest
INSERT INTO tenant_supermarket2.planning
    (address_id, user_id, plan_date, disponibility)
SELECT a.id, u.id, d.plan_date,
    '[{"start":"09:00","end":"12:00"},{"start":"14:00","end":"18:00"}]'::jsonb
FROM tenant_supermarket2.addresses a
CROSS JOIN public.users u
CROSS JOIN (
    SELECT CURRENT_DATE + 1 AS plan_date
    UNION ALL SELECT CURRENT_DATE + 2
    UNION ALL SELECT CURRENT_DATE + 3
) d
WHERE (a.label = 'SM2 Paris' AND u.email = 'emp2a@sm2.test')
   OR (a.label = 'SM2 Brest' AND u.email = 'emp2b@sm2.test');

COMMIT;


-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
-- 3c. Student Residence  (Paris + Rennes + Brest · 3 studios each)
-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
BEGIN;
SELECT set_config('app.user_id',
    (SELECT id::text FROM public.users WHERE email = 'admin@residence.test'),
    false);
SELECT set_config('app.active_role', 'TENANT_ADMIN', false);

-- Addresses
INSERT INTO tenant_student_residence.addresses (label, street_address, postal_code, city) VALUES
    ('Résidence Paris',  '5 Rue des Étudiants',              '75005', 'Paris'),
    ('Résidence Rennes', '15 Avenue du Général Leclerc',     '35000', 'Rennes'),
    ('Résidence Brest',  '3 Rue de la Recouvrance',          '29200', 'Brest');

-- address_admins:
--   admin@residence.test (TENANT_ADMIN) also manages all 3 addresses
--   addradmin@residence.test (STORE_ADMIN) manages all 3 addresses
INSERT INTO tenant_student_residence.address_admins (address_id, user_id)
SELECT a.id, u.id
FROM tenant_student_residence.addresses a
CROSS JOIN public.users u
WHERE u.email IN ('admin@residence.test', 'addradmin@residence.test');

-- Studios: 3 per address, process_id = NULL, shared = false
INSERT INTO tenant_student_residence.studios
    (address_id, label, surface_m2, monthly_rent, status, shared, active)
SELECT a.id, v.label, v.surface_m2, v.monthly_rent, 'NOT_SHARED', FALSE, TRUE
FROM tenant_student_residence.addresses a
JOIN (VALUES
    ('Résidence Paris',  'Studio T1 — Bat A', 18.0, 620.00),
    ('Résidence Paris',  'Studio T2 — Bat B', 22.0, 750.00),
    ('Résidence Paris',  'Studio T3 — Bat C', 25.0, 890.00),
    ('Résidence Rennes', 'Studio T1 — Bat A', 17.0, 480.00),
    ('Résidence Rennes', 'Studio T2 — Bat B', 20.0, 550.00),
    ('Résidence Rennes', 'Studio T3 — Bat C', 24.0, 640.00),
    ('Résidence Brest',  'Studio T1 — Bat A', 19.0, 420.00),
    ('Résidence Brest',  'Studio T2 — Bat B', 21.0, 490.00),
    ('Résidence Brest',  'Studio T3 — Bat C', 26.0, 560.00)
) AS v(addr_label, label, surface_m2, monthly_rent) ON a.label = v.addr_label;

COMMIT;


-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
-- 3d. Association  (Lyon + Marseille + Bordeaux · 3 activities each)
-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
BEGIN;
SELECT set_config('app.user_id',
    (SELECT id::text FROM public.users WHERE email = 'admin@asso.test'),
    false);
SELECT set_config('app.active_role', 'TENANT_ADMIN', false);

-- Addresses
INSERT INTO tenant_association.addresses (label, street_address, postal_code, city) VALUES
    ('Asso Lyon',      '12 Rue de la République', '69001', 'Lyon'),
    ('Asso Marseille', '20 La Canebière',          '13001', 'Marseille'),
    ('Asso Bordeaux',  '7 Place de la Victoire',   '33000', 'Bordeaux');

-- address_admins: addradmin@asso.test manages all 3 addresses
INSERT INTO tenant_association.address_admins (address_id, user_id)
SELECT a.id, u.id
FROM tenant_association.addresses a
CROSS JOIN public.users u
WHERE u.email = 'addradmin@asso.test';

-- Activities: 3 per address, process_id = NULL, shared = false
INSERT INTO tenant_association.activities
    (address_id, title, description, starts_at, ends_at, status, shared, active)
SELECT a.id, v.title, v.description,
    NOW() + v.start_offset,
    NOW() + v.start_offset + v.duration,
    'NOT_SHARED', FALSE, TRUE
FROM tenant_association.addresses a
JOIN (VALUES
    -- Lyon
    ('Asso Lyon',      'Atelier CV et entretien',
        'Préparation aux entretiens — inscription par email',
        INTERVAL '7 days',  INTERVAL '2 hours'),
    ('Asso Lyon',      'Permanence juridique',
        'Aide juridique gratuite pour étudiants',
        INTERVAL '14 days', INTERVAL '3 hours'),
    ('Asso Lyon',      'Soirée networking',
        'Rencontre entre étudiants et professionnels',
        INTERVAL '21 days', INTERVAL '4 hours'),
    -- Marseille
    ('Asso Marseille', 'Cours de français langue étrangère',
        'Soutien linguistique pour étudiants internationaux',
        INTERVAL '5 days',  INTERVAL '2 hours'),
    ('Asso Marseille', 'Aide aux devoirs',
        'Tutorat gratuit — toutes matières',
        INTERVAL '10 days', INTERVAL '3 hours'),
    ('Asso Marseille', 'Sortie culturelle — musée',
        'Visite guidée à tarif réduit',
        INTERVAL '30 days', INTERVAL '5 hours'),
    -- Bordeaux
    ('Asso Bordeaux',  'Atelier cuisine solidaire',
        'Recettes économiques et anti-gaspi',
        INTERVAL '3 days',  INTERVAL '3 hours'),
    ('Asso Bordeaux',  'Conférence — vie étudiante',
        'Témoignages et conseils pratiques',
        INTERVAL '15 days', INTERVAL '2 hours'),
    ('Asso Bordeaux',  'Marché solidaire',
        'Vente de produits à prix libre — bénéfice associations',
        INTERVAL '8 days',  INTERVAL '4 hours')
) AS v(addr_label, title, description, start_offset, duration)
ON a.label = v.addr_label;

COMMIT;


-- =============================================================================
-- 4. VERIFY
-- =============================================================================

\echo ''
\echo '=== public row counts ==='
SELECT tbl, n FROM (
    SELECT 'users'             AS tbl, COUNT(*)::text AS n FROM public.users
    UNION ALL SELECT 'tenants',             COUNT(*)::text FROM public.tenants
    UNION ALL SELECT 'tenant_modules',      COUNT(*)::text FROM public.tenant_modules
    UNION ALL SELECT 'tenant_memberships',  COUNT(*)::text FROM public.tenant_memberships
    UNION ALL SELECT 'user_roles',          COUNT(*)::text FROM public.user_roles
    UNION ALL SELECT 'process',             COUNT(*)::text FROM public.process
) t ORDER BY tbl;

\echo ''
\echo '=== tenants + modules ==='
SELECT t.name, t.tenant_type, t.schema_name, m.module_key
FROM public.tenants t
LEFT JOIN public.tenant_modules m ON m.tenant_id = t.id
ORDER BY t.name, m.module_key;

\echo ''
\echo '=== users + roles ==='
SELECT u.email, r.role, COALESCE(t.schema_name, '(platform)') AS tenant
FROM public.users u
JOIN public.user_roles r  ON r.user_id  = u.id
LEFT JOIN public.tenants t ON t.id      = r.tenant_id
ORDER BY u.email, r.role;

\echo ''
\echo '=== SM1 addresses + products ==='
SELECT set_config('app.user_id',
    (SELECT id::text FROM public.users WHERE email = 'admin1@sm1.test'), false);
SELECT a.label AS address, p.name AS product, pb.expiry_date, pb.quantity
FROM tenant_supermarket1.addresses a
JOIN tenant_supermarket1.products p ON p.address_id = a.id
JOIN tenant_supermarket1.product_batches pb ON pb.product_id = p.id
ORDER BY a.label, p.name, pb.expiry_date;

\echo ''
\echo '=== SM2 addresses + products ==='
SELECT set_config('app.user_id',
    (SELECT id::text FROM public.users WHERE email = 'admin2@sm2.test'), false);
SELECT a.label AS address, p.name AS product, pb.expiry_date, pb.quantity
FROM tenant_supermarket2.addresses a
JOIN tenant_supermarket2.products p ON p.address_id = a.id
JOIN tenant_supermarket2.product_batches pb ON pb.product_id = p.id
ORDER BY a.label, p.name, pb.expiry_date;

\echo ''
\echo '=== Residence studios ==='
SELECT set_config('app.user_id',
    (SELECT id::text FROM public.users WHERE email = 'admin@residence.test'), false);
SELECT a.label AS address, s.label AS studio, s.surface_m2, s.monthly_rent, s.shared
FROM tenant_student_residence.addresses a
JOIN tenant_student_residence.studios s ON s.address_id = a.id
ORDER BY a.label, s.label;

\echo ''
\echo '=== Association activities ==='
SELECT set_config('app.user_id',
    (SELECT id::text FROM public.users WHERE email = 'admin@asso.test'), false);
SELECT a.label AS address, ac.title, ac.starts_at::date AS date, ac.shared
FROM tenant_association.addresses a
JOIN tenant_association.activities ac ON ac.address_id = a.id
ORDER BY a.label, ac.starts_at;

\echo ''
\echo '=== SM1 planning ==='
SELECT set_config('app.user_id',
    (SELECT id::text FROM public.users WHERE email = 'admin1@sm1.test'), false);
SELECT u.email, a.label AS address, pl.plan_date
FROM tenant_supermarket1.planning pl
JOIN public.users u ON u.id = pl.user_id
JOIN tenant_supermarket1.addresses a ON a.id = pl.address_id
ORDER BY u.email, pl.plan_date;

\echo ''
\echo '=== SM2 planning ==='
SELECT set_config('app.user_id',
    (SELECT id::text FROM public.users WHERE email = 'admin2@sm2.test'), false);
SELECT u.email, a.label AS address, pl.plan_date
FROM tenant_supermarket2.planning pl
JOIN public.users u ON u.id = pl.user_id
JOIN tenant_supermarket2.addresses a ON a.id = pl.address_id
ORDER BY u.email, pl.plan_date;

\echo ''
\echo '=== setup complete ==='
\echo 'Run order:  1) scripts/recreate-tenants.sql (postgres)  2) sql/setup.sql (app_rls)'
