#!/bin/sh
set -eu

# Starts the notif-db container using the outer docker-compose.yml, waits
# until Postgres accepts connections, ensures the application role exists
# with the configured password, and bootstraps the schema idempotently.

# By default use the outer repository docker-compose.yml (one level up).
# Override by exporting COMPOSE_FILE before running.
COMPOSE_FILE=${COMPOSE_FILE:-../docker-compose.yml}

# Load .env from repo root if present (so users can keep NOTIF_DB_* in .env)
if [ -f "${COMPOSE_FILE%/*}/.env" ]; then
  # shellcheck disable=SC1090
  set -a; . "${COMPOSE_FILE%/*}/.env"; set +a
fi

# Defaults (can be overridden by exporting these before running the script)
: "${NOTIF_DB_NAME:=notifdb}"
: "${NOTIF_DB_USER:=notif_user}"
: "${NOTIF_DB_PASSWORD:=notif_pass}"
# Docker network to attach the DB to so other containers can resolve 'notif-db'
: "${NETWORK:=fitflow-net}"

export NOTIF_DB_NAME NOTIF_DB_USER NOTIF_DB_PASSWORD COMPOSE_FILE
export NETWORK

# If --reinit provided, remove volumes so Postgres does a fresh init (DANGEROUS)
if [ "${1:-}" = "--reinit" ]; then
  echo "Reinitializing DB: stopping containers and removing volumes (this deletes data)"
  docker compose -f "$COMPOSE_FILE" down -v || true
fi

echo "Bringing up notif-db..."
docker compose -f "$COMPOSE_FILE" up -d notif-db

# Ensure the Docker network exists and attach the container to it so other
# containers not launched with the same compose project can resolve 'notif-db'.
if ! docker network inspect "$NETWORK" >/dev/null 2>&1; then
  echo "Creating docker network $NETWORK"
  docker network create "$NETWORK"
fi

# Connect notif-db container to the desired network if it's not already attached
CONTAINER_ID=$(docker compose -f "$COMPOSE_FILE" ps -q notif-db)
if [ -n "$CONTAINER_ID" ]; then
  if ! docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}' "$CONTAINER_ID" | grep -qw "$NETWORK"; then
    echo "Connecting notif-db ($CONTAINER_ID) to network $NETWORK"
    docker network connect "$NETWORK" "$CONTAINER_ID" || true
  else
    echo "notif-db already connected to $NETWORK"
  fi
else
  echo "Warning: could not determine notif-db container id" >&2
fi

echo "Waiting for Postgres process and accepting connections..."
# Wait until either the app user or the container's superuser accepts connections
MAX_WAIT=60
count=0
while ! docker compose -f "$COMPOSE_FILE" exec -T notif-db pg_isready -U "$NOTIF_DB_USER" -d "$NOTIF_DB_NAME" >/dev/null 2>&1; do
  if docker compose -f "$COMPOSE_FILE" exec -T -u postgres notif-db pg_isready -U postgres -d "$NOTIF_DB_NAME" >/dev/null 2>&1; then
    break
  fi
  sleep 1
  count=$((count+1))
  if [ $count -ge $MAX_WAIT ]; then
    echo "Timed out waiting for Postgres to be ready" >&2
    exit 1
  fi
done

# Determine DB superuser name inside the container (fallback to 'postgres')
DB_SUPERUSER=$(docker compose -f "$COMPOSE_FILE" exec -T notif-db printenv POSTGRES_USER 2>/dev/null || echo postgres)

echo "Ensuring application DB user exists and password is correct..."
# Try to connect as app user; if it fails, create/alter the role as postgres superuser
if docker compose -f "$COMPOSE_FILE" exec -T notif-db psql -U "$NOTIF_DB_USER" -d "$NOTIF_DB_NAME" -c '\q' >/dev/null 2>&1; then
  echo "App user can connect with provided credentials"
else
  echo "App user cannot connect; creating or updating role as postgres superuser"
  docker compose -f "$COMPOSE_FILE" exec -T -u postgres notif-db psql -U "$DB_SUPERUSER" -v ON_ERROR_STOP=1 -d "$NOTIF_DB_NAME" <<EOSQL
DO \\$\
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${NOTIF_DB_USER}') THEN
    PERFORM pg_sleep(0); -- placeholder
    EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', '${NOTIF_DB_USER}', '${NOTIF_DB_PASSWORD}');
  ELSE
    EXECUTE format('ALTER ROLE %I WITH PASSWORD %L', '${NOTIF_DB_USER}', '${NOTIF_DB_PASSWORD}');
  END IF;
END
\\\$\
;
GRANT CONNECT ON DATABASE "${NOTIF_DB_NAME}" TO "${NOTIF_DB_USER}";
GRANT USAGE ON SCHEMA public TO "${NOTIF_DB_USER}";
EOSQL
fi

echo "Bootstrapping schema (idempotent)..."
docker compose -f "$COMPOSE_FILE" exec -T -u postgres notif-db psql -U "$DB_SUPERUSER" -v ON_ERROR_STOP=1 -d "$NOTIF_DB_NAME" <<EOSQL
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created_at
    ON notifications (user_id, created_at DESC);
EOSQL

# Grant permissions to the app user
docker compose -f "$COMPOSE_FILE" exec -T -u postgres notif-db psql -U "$DB_SUPERUSER" -d "$NOTIF_DB_NAME" -c "GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE notifications TO \"${NOTIF_DB_USER}\";"

echo "notif-db is ready."
