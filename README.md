# Student Marketplace (B2B2C)

PostgreSQL multi-tenant schema + Spring Boot JWT API.

## Prerequisites

- PostgreSQL database `student_marketplace`
- **First-time DB:** run as postgres superuser: `scripts/recreate-tenants.sql`, then as `app_rls`: `sql/setup.sql`
- Java 21, Maven

## Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Datasource password is quoted in `application-dev.yml` (`"0000"`).

Staff API calls need headers when you have multiple roles: `X-Active-Role`, `X-Tenant-Id`.

## Authentication

Use the API or Swagger UI for email OTP or password login.

- Request an OTP with `POST /api/auth/otp/request`, then verify it with `POST /api/auth/otp/verify`.
- In development, read OTP codes in **Mailpit** at http://localhost:8025 when SMTP is on port 1025.
- For password login, use `POST /api/auth/login` with a seeded demo email and `Password123!`.
- Send the returned JWT as `Authorization: Bearer <token>` on authenticated requests.

## Demo personas 

| Label | Email |
|--------|--------|
| SM1 — tenant admin | `admin1@sm1.test` |
| SM1 — store admin | `storeadmin1@sm1.test` |
| SM1 — employee | `emp1a@sm1.test` |
| SM1 — employee + student | `emp1b@sm1.test` |
| SM2 — tenant admin | `admin2@sm2.test` |
| SM2 — store admin | `storeadmin2@sm2.test` |
| SM2 — employee | `emp2a@sm2.test` |
| SM2 — employee + student | `emp2b@sm2.test` |
| Residence — tenant admin | `admin@residence.test` |
| Residence — address admin | `addradmin@residence.test` |
| Association — tenant admin | `admin@asso.test` |
| Association — address admin | `addradmin@asso.test` |
| Association — member | `member1@asso.test`, `member2@asso.test` |

Set `X-Active-Role` to select a role when the JWT contains multiple roles.

## API smoke test

With the backend running:

```bash
./scripts/smoke-api.sh
```

Optional: `BASE_URL=http://localhost:8080 ./scripts/smoke-api.sh`
