#!/usr/bin/env sh
set -eu

# Idempotent start/stop script for mcp-server
# Usage: ./start.sh [start|stop|restart]

ACTION=${1:-start}

IMG_NAME=fitflow/mcp-server:latest
CONTAINER_NAME=mcp-server
HOST_PORT=7777
CONTAINER_PORT=8000
NETWORK_NAME=fitflow-net
# Prefer a compose-created, project-prefixed network (e.g. "<project>_fitflow-net").
# If none found, fall back to literal "fitflow-net" when present.
if command -v docker >/dev/null 2>&1; then
  _prefixed=$(docker network ls --format '{{.Name}}' | grep -E '_fitflow[-_]?net$' | head -n1 || true)
  if [ -n "$_prefixed" ]; then
    NETWORK_NAME="$_prefixed"
  else
    _plain=$(docker network ls --format '{{.Name}}' | grep -E '^fitflow[-_]?net$' | head -n1 || true)
    if [ -n "$_plain" ]; then
      NETWORK_NAME="$_plain"
    fi
  fi
fi
# Inspector container ports: use alternate container-side ports so an in-
# container forwarder can listen there without conflicting with the
# inspector process which binds 127.0.0.1:6274 inside the container.
INSPECTOR_CONTAINER_PORT=16274
INSPECTOR_PROXY_CONTAINER_PORT=16277
# Map the host ports the inspector advertises (6274 UI, 6277 proxy)
INSPECTOR_HOST_PORT_UI=6274
INSPECTOR_HOST_PORT_PROXY=6277
BUILD_DIR="$(cd "$(dirname "$0")" && pwd)"

log() { printf '%s\n' "$*"; }

container_exists() {
  docker ps -aq -f name="^/${CONTAINER_NAME}$" | grep -q . 2>/dev/null
}

container_running() {
  docker ps -q -f name="^/${CONTAINER_NAME}$" | grep -q . 2>/dev/null
}

stop_container() {
  if container_running; then
    log "Stopping running container $CONTAINER_NAME..."
    docker stop "$CONTAINER_NAME" || true
  fi
  if container_exists; then
    log "Removing container $CONTAINER_NAME..."
    docker rm -f "$CONTAINER_NAME" || true
  fi
}

remove_image() {
  if docker images -q "$IMG_NAME" | grep -q . 2>/dev/null; then
    log "Removing image $IMG_NAME..."
    docker rmi -f "$IMG_NAME" || true
  fi
}

do_start() {
  log "Preparing fresh container runtime for $CONTAINER_NAME"
  stop_container
  remove_image

  # Ensure the compose network exists so Docker DNS works when running via start.sh
  if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
    log "ERROR: Network '$NETWORK_NAME' not found."
    log "This script will not create networks automatically — attach to an existing compose network."
    log "Create or start the compose network (example): 'docker-compose up -d consul booking-svc' or create network manually with: 'docker network create <network-name>'"
    exit 1
  else
    log "Using existing network: $NETWORK_NAME"
  fi

  log "Building image $IMG_NAME from $BUILD_DIR"
  docker build -t "$IMG_NAME" "$BUILD_DIR"

  log "Starting container $CONTAINER_NAME (host:$HOST_PORT -> container:$CONTAINER_PORT, host:${INSPECTOR_HOST_PORT_UI}:${INSPECTOR_CONTAINER_PORT} -> container:${INSPECTOR_CONTAINER_PORT}, host:${INSPECTOR_HOST_PORT_PROXY}:${INSPECTOR_PROXY_CONTAINER_PORT} -> container:${INSPECTOR_PROXY_CONTAINER_PORT})"
  docker run -d --name "$CONTAINER_NAME" \
    --network "$NETWORK_NAME" \
    -p "${HOST_PORT}:${CONTAINER_PORT}" \
    -p "${INSPECTOR_HOST_PORT_UI}:${INSPECTOR_CONTAINER_PORT}" \
    -p "${INSPECTOR_HOST_PORT_PROXY}:${INSPECTOR_PROXY_CONTAINER_PORT}" \
    "$IMG_NAME"
  log "Container started: $(docker ps -f name="$CONTAINER_NAME" --format '{{.Names}} {{.Status}}')"
}

do_stop() {
  log "Stopping and removing container and image for $CONTAINER_NAME"
  stop_container
  remove_image
  log "Done."
}

case "$ACTION" in
  start)
    do_start
    ;;
  stop)
    do_stop
    ;;
  restart)
    do_stop
    do_start
    ;;
  *)
    echo "Usage: $0 [start|stop|restart]"
    exit 2
    ;;
esac
