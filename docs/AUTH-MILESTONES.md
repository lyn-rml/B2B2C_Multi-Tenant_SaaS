# Auth milestones (M1–M4)

| Milestone | Status | Notes |
|-----------|--------|--------|
| M1 Bootstrap + JPA | Done | `GET /api/health`, `public` entities, `ddl-auto: validate` |
| M2 Password + JWT | Done | `POST /api/auth/register`, `/login`, `GET /api/auth/me` |
| M3 Email OTP | Done | `POST /api/auth/otp/request`, `/otp/verify`; Mailpit SMTP `localhost:1025` |
| M4 RBAC | Done | Current database roles, active account checks and tenant membership; US-000/US-001 guards |

## Dev mail (OTP)

```bash
docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
```

## Authentication and tenant access

Use `Authorization: Bearer <jwt>`. The backend reloads account activation and roles for every authenticated request. Tenant access also checks the current membership and a scoped staff role. `X-Active-Role` and `X-Tenant-Id` no longer grant or select access.

Public registration grants only `STUDENT`. Login and OTP work for the five roles. No account or module is created automatically. The first Platform Admin is designated manually by the database owner.

The former business APIs and dashboards have been removed. The current administration API implements US-000 and US-001. See [the setup and API guide](BACKEND-US000-US001.md) for payloads, manual account setup and database permissions.

Configure the datasource in `backend/src/main/resources/application-dev.yml` or through Spring datasource environment variables. The initial installation uses `docs/script.sql`. Tenant tables are provisioned on module assignment. Neither the installation nor the backend adds RLS policies.
