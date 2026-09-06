#!/usr/bin/env sh
set -eu

# Stop and remove the mcp-server container and image (idempotent)

CONTAINER_NAME=mcp-server
IMG_NAME=fitflow/mcp-server:latest

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

log "Stopping and cleaning mcp-server"
stop_container
remove_image
log "Finished."
