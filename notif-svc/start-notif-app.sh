#!/usr/bin/env sh
set -e

# Start the notif app container (idempotent). Creates network if missing.
cd "$(dirname "$0")" || exit 1

# Load .env if present
if [ -f .env ]; then
  set -o allexport
  # shellcheck disable=SC1091
  . .env
  set +o allexport
fi

SERVER_PORT=${SERVER_PORT:-8002}
DB_HOST=${DB_HOST:-notif-db}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-notifdb}
DB_USER=${DB_USER:-notif_user}
DB_PASSWORD=${DB_PASSWORD:-notif_pass}
IMAGE=${IMAGE:-fitflow/notif-svc:latest}
NAME=${NAME:-notif-svc}
NETWORK=${NETWORK:-fitflow-net}

# Ensure IMAGE is not empty (protect against empty .env overrides) and show debug info
: ${IMAGE:="fitflow/notif-svc:latest"}
if [ -z "$IMAGE" ]; then
  echo "ERROR: IMAGE is empty. Set IMAGE or remove empty IMAGE= from .env"
  exit 1
fi

echo "Starting container with: IMAGE=$IMAGE NAME=$NAME NETWORK=$NETWORK DB_HOST=$DB_HOST DB_PORT=$DB_PORT"
echo "Passing NOTIF_DB_* envs to container so Spring uses the correct DB host/port"

if ! docker network inspect "$NETWORK" >/dev/null 2>&1; then
  docker network create "$NETWORK"
fi

# Auto-attach a running Postgres DB container to the network with alias 'notif-db'
# so the app can resolve the DB without exporting NETWORK on the command line.
DB_CONTAINER=""
DB_CONTAINER=$(docker ps -q -f name=notif-db 2>/dev/null || true)
if [ -z "$DB_CONTAINER" ]; then
  DB_CONTAINER=$(docker ps -q -f name=fitflow-notif-db-1 2>/dev/null || true)
fi
if [ -z "$DB_CONTAINER" ]; then
  DB_CONTAINER=$(docker ps -q --filter ancestor=postgres:16-alpine 2>/dev/null | head -n 1 || true)
fi
if [ -n "$DB_CONTAINER" ]; then
  if ! docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}' "$DB_CONTAINER" | grep -qw "$NETWORK"; then
    echo "Connecting DB container $DB_CONTAINER to network $NETWORK with alias notif-db"
    docker network connect --alias notif-db "$NETWORK" "$DB_CONTAINER" || true
  else
    echo "DB container $DB_CONTAINER already attached to $NETWORK"
  fi
else
  echo "No running Postgres DB container detected to attach to $NETWORK (continuing)"
fi

# Resolve DB IP on the selected network and add an /etc/hosts entry for 'notif-db'
ADD_HOST_ARG=""
if [ -n "$DB_CONTAINER" ]; then
  DB_IP=$(docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}={{$v.IPAddress}};{{end}}' "$DB_CONTAINER" 2>/dev/null | tr ';' '\n' | awk -F= -v net="$NETWORK" '$1==net{print $2}' )
  if [ -n "$DB_IP" ]; then
    echo "Resolved DB IP on network $NETWORK: $DB_IP — adding --add-host notif-db:$DB_IP"
    ADD_HOST_ARG="--add-host notif-db:$DB_IP"
  else
    echo "Could not resolve DB IP for $DB_CONTAINER on $NETWORK"
  fi
fi

# If already running, exit
if [ "$(docker ps -q -f name=^/${NAME}$)" ]; then
  echo "$NAME is already running"
  exit 0
fi

# Remove stopped container with same name
if [ "$(docker ps -aq -f status=exited -f name=^/${NAME}$)" ]; then
  docker rm "$NAME" >/dev/null || true
fi

docker run -d --name "$NAME" \
  ${ADD_HOST_ARG:+$ADD_HOST_ARG} \
  --network "$NETWORK" \
  -p "${SERVER_PORT}:${SERVER_PORT}" \
  -e SERVER_PORT="$SERVER_PORT" \
  -e DB_HOST="$DB_HOST" \
  -e DB_PORT="$DB_PORT" \
  -e DB_NAME="$DB_NAME" \
  -e DB_USER="$DB_USER" \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e NOTIF_DB_HOST="$DB_HOST" \
  -e NOTIF_DB_PORT="$DB_PORT" \
  -e NOTIF_DB_NAME="$DB_NAME" \
  -e NOTIF_DB_USER="$DB_USER" \
  -e NOTIF_DB_PASSWORD="$DB_PASSWORD" \
  "$IMAGE"

echo "Started $NAME using image $IMAGE"
