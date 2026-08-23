#!/bin/sh
set -eu

# Stops and removes the notif-db container started by notif-svc/docker-compose.yml


# By default use the outer repository docker-compose.yml (one level up).
# Override by exporting COMPOSE_FILE before running the script.
COMPOSE_FILE=${COMPOSE_FILE:-../docker-compose.yml}

echo "Stopping notif-db container..."
docker compose -f "$COMPOSE_FILE" stop notif-db || true

echo "Removing notif-db container (if present)..."
docker compose -f "$COMPOSE_FILE" rm -f notif-db || true

echo "Finished."
