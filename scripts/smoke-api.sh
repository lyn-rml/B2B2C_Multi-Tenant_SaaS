#!/usr/bin/env bash
# Quick API smoke test against local backend (http://localhost:8080).
# Requires: curl, jq, seeded DB (sql/setup.sql) and running Spring Boot.

set -euo pipefail
BASE="${BASE_URL:-http://localhost:8080}"
SM1="aa000001-0000-4000-8000-000000000001"
SM2="aa000002-0000-4000-8000-000000000002"
RES="aa000003-0000-4000-8000-000000000003"
PASS='Password123!'

echo "== health =="
curl -sf "$BASE/api/health" | jq .

login() {
  local email="$1"
  curl -sf -X POST "$BASE/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"$PASS\"}" | jq -r .token
}

echo "== TENANT_ADMIN (SM1) =="
TOKEN=$(login 'admin1@sm1.test')
curl -sf -H "Authorization: Bearer $TOKEN" -H 'X-Active-Role: TENANT_ADMIN' -H "X-Tenant-Id: $SM1" \
  "$BASE/api/admin/tenants" | jq 'length'
curl -sf -H "Authorization: Bearer $TOKEN" -H 'X-Active-Role: TENANT_ADMIN' -H "X-Tenant-Id: $SM1" \
  "$BASE/api/admin/addresses" | jq 'length'
curl -sf -H "Authorization: Bearer $TOKEN" -H 'X-Active-Role: TENANT_ADMIN' -H "X-Tenant-Id: $SM1" \
  "$BASE/api/tenant/$SM1/modules" | jq .

echo "== STORE_ADMIN (SM1) =="
TOKEN=$(login 'storeadmin1@sm1.test')
curl -sf -H "Authorization: Bearer $TOKEN" -H 'X-Active-Role: STORE_ADMIN' -H "X-Tenant-Id: $SM1" \
  "$BASE/api/store/dashboard" | jq '.title, (.items | length)'

echo "== EMPLOYEE (SM1) =="
TOKEN=$(login 'emp1a@sm1.test')
curl -sf -H "Authorization: Bearer $TOKEN" -H 'X-Active-Role: EMPLOYEE' -H "X-Tenant-Id: $SM1" \
  "$BASE/api/employee/planning" | jq 'length'

echo "== STUDENT (dual role user) =="
TOKEN=$(login 'emp1b@sm1.test')
curl -sf -H "Authorization: Bearer $TOKEN" -H 'X-Active-Role: STUDENT' \
  "$BASE/api/student/offers" | jq 'length'

echo "== Residence tenant admin (studios module) =="
TOKEN=$(login 'admin@residence.test')
curl -sf -H "Authorization: Bearer $TOKEN" -H 'X-Active-Role: TENANT_ADMIN' -H "X-Tenant-Id: $RES" \
  "$BASE/api/tenant/$RES/modules" | jq .

echo "== smoke OK =="
