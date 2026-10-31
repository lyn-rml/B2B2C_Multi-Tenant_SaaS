#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
pg_bin="${PG_BIN:-$(pg_config --bindir)}"
test_dir="$(mktemp -d /tmp/marketplace-pg-test.XXXXXX)"
cleanup() {
  "$pg_bin/pg_ctl" -D "$test_dir/data" -m immediate -w stop >/dev/null 2>&1 || true
  rm -rf -- "$test_dir"
}
trap cleanup EXIT
# This cluster and all test data are disposable. Never connect to the configured application database.
"$pg_bin/initdb" -D "$test_dir/data" -U marketplace_owner --auth=trust --no-locale >"$test_dir/init.log"
test_port="$(python3 - <<'PY'
import socket
with socket.socket() as sock:
    sock.bind(('127.0.0.1', 0))
    print(sock.getsockname()[1])
PY
)"
"$pg_bin/pg_ctl" -D "$test_dir/data" -l "$test_dir/postgres.log" \
  -o "-h 127.0.0.1 -p $test_port -k $test_dir" -w start >/dev/null
export PGHOST=127.0.0.1 PGPORT="$test_port" PGUSER=marketplace_owner
"$pg_bin/createdb" marketplace_test
"$pg_bin/psql" -X -v ON_ERROR_STOP=1 -d marketplace_test -f "$repo_root/docs/script.sql" >/dev/null
"$pg_bin/psql" -X -v ON_ERROR_STOP=1 -d marketplace_test >/dev/null <<'SQL'
CREATE ROLE marketplace_test_app LOGIN;
GRANT CREATE ON DATABASE marketplace_test TO marketplace_test_app;
GRANT USAGE ON SCHEMA public TO marketplace_test_app;
GRANT ALL ON ALL TABLES IN SCHEMA public TO marketplace_test_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO marketplace_test_app;
SQL
export MARKETPLACE_TEST_JDBC_URL="jdbc:postgresql://127.0.0.1:$test_port/marketplace_test"
mvn -f "$repo_root/backend/pom.xml" clean test "$@"
