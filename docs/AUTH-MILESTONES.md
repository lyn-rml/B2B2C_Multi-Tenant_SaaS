# Auth milestones (M1–M4)

| Milestone | Status | Notes |
|-----------|--------|--------|
| M1 Bootstrap + JPA | Done | `GET /api/health`, `public` entities, `ddl-auto: validate` |
| M2 Password + JWT | Done | `POST /api/auth/register`, `/login`, `GET /api/auth/me` |
| M3 Email OTP | Done | `POST /api/auth/otp/request`, `/otp/verify`; Mailpit SMTP `localhost:1025` |
| M4 RBAC | Done | `@PreAuthorize` on admin/role dashboards; URL guards retained |

## Dev mail (OTP)

```bash
docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
```

## RLS + active role headers

Staff tenant APIs expect:

- `Authorization: Bearer <jwt>`
- `X-Active-Role`: `TENANT_ADMIN` | `EMPLOYEE` | `STORE_ADMIN`
- `X-Tenant-Id`: UUID (optional when only one tenant for that role)

Student routes under `/api/student/**` force active role `STUDENT`.

Datasource uses PostgreSQL role `app_rls`. See the root README for the empty-schema installation, explicit first Platform Admin bootstrap, and database checks. Public registration grants only `STUDENT`.
