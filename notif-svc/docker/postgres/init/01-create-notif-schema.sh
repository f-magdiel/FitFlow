#!/bin/sh
set -eu

: "${NOTIF_DB_USER:?NOTIF_DB_USER is required}"
: "${NOTIF_DB_PASSWORD:?NOTIF_DB_PASSWORD is required}"

# Use POSTGRES_DB if provided by container, otherwise fall back to NOTIF_DB_NAME
DB_NAME=${POSTGRES_DB:-${NOTIF_DB_NAME}}

psql \
    --username "$POSTGRES_USER" \
    --dbname "$DB_NAME" \
    --set=app_user="$NOTIF_DB_USER" \
    --set=app_password="$NOTIF_DB_PASSWORD" \
    --set=app_db="$DB_NAME" <<'EOSQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_user', :'app_password')
WHERE NOT EXISTS (
    SELECT 1 FROM pg_roles WHERE rolname = :'app_user'
) \gexec

GRANT CONNECT ON DATABASE :"app_db" TO :"app_user";
GRANT USAGE ON SCHEMA public TO :"app_user";

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

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE notifications TO :"app_user";
EOSQL
