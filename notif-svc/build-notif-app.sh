#!/usr/bin/env sh
set -e

cd "$(dirname "$0")" || exit 1

IMAGE=${IMAGE:-fitflow/notif-svc:latest}
NETWORK=${NETWORK:-fitflow-net}

echo "Ensuring docker network $NETWORK exists"
docker network inspect "$NETWORK" >/dev/null 2>&1 || docker network create "$NETWORK"

echo "Building $IMAGE from $(pwd)"
docker build -t "$IMAGE" .

echo "Built $IMAGE"
