# Student Marketplace (B2B2C)

PostgreSQL 17 database structure and Spring Boot JWT API.

The target model is documented in [Database-Schema-LiveV1.md](Database-Schema-LiveV1.md), with scenarios in [User-Stories-LiveV1.md](User-Stories-LiveV1.md).

## Database installation

The initial database installation script is [docs/script.sql](docs/script.sql). Run it once in an empty database that you have already created, using its owner connection:

```bash
psql -X -v ON_ERROR_STOP=1 -d YOUR_DATABASE -f docs/script.sql
```

It creates nine empty tables in the existing `public` schema. It does not create databases, tenant schemas, database roles, RLS policies, functions, triggers or seed data. If you have already run it successfully, keep the file as the structure reference; it is not an update script to rerun against existing tables.

The remaining SQL sections document the tenant table templates. The backend now provisions them through the module-assignment API, using versioned resources in `backend/src/main/resources/db/tenant/`:

| Scope | Tables |
|-------|--------|
| Common | `addresses`, `address_admins`, `employee_addresses` |
| `PRODUCTS_BASKETS` | `products`, `product_batches`, `baskets`, `basket_items` |
| `STUDIOS` | `studios` |
| `ACTIVITIES` | `activities` |

The module catalog is initially empty. A Platform Admin creates its entries through the API, then assigns modules to tenants. The assignment API creates each tenant schema, its common tables and only the selected module tables, atomically and without seed data. Direct SQL inserts into `tenant_modules` do not provision tables. Scheduled publication, reservation stock transactions and the other business APIs are outside the current scope.

## Database connection

The `dev` profile is active by default in `backend/src/main/resources/application.yml`. Set your connection in [application-dev.yml](backend/src/main/resources/application-dev.yml):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/YOUR_DATABASE
    username: YOUR_DATABASE_USER
    password: YOUR_DATABASE_PASSWORD
```

Use the actual database name, host, port and credentials. The script does not create a login. The environment variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` override these file settings. Restart the backend after changing the connection.

Hibernate uses `ddl-auto: validate`: the public tables must exist before startup; Hibernate does not create them automatically.

## Backend

Requires Java 21 and Maven.

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

API: `http://localhost:8080` · Swagger: `http://localhost:8080/swagger-ui.html`.

The backend exposes authentication and the US-000/US-001 tenant and module APIs. Previous business APIs and dashboards have been removed. Use `Authorization: Bearer <token>`; role and tenant headers are no longer used. Current account status, roles and tenant membership are checked in the database. Public registration grants only `STUDENT`. No administrator, module or demonstration account is installed automatically. Development OTP mail is delivered to Mailpit when configured.

See [the implementation and setup guide](docs/BACKEND-US000-US001.md) for endpoint payloads, manual first-admin setup, provisioning behavior and required database permissions. The configured `app_rls` account currently needs permissions from the owner of `B2B_Market` before these APIs can operate; the guide contains the commands. No RLS policies are required.

## Verification

```bash
mvn -f backend/pom.xml test
# Full HTTP integration suite against a disposable PostgreSQL cluster:
backend/scripts/test-postgres.sh
```

With the backend running, `GET /api/health` checks application health and `GET /v3/api-docs` returns the API description.
