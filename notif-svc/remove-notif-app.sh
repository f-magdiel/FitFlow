#!/usr/bin/env sh
set -e

NAME=${NAME:-notif-svc}
IMAGE=${IMAGE:-fitflow/notif-svc:latest}
NETWORK=${NETWORK:-fitflow-net}

if [ "$(docker ps -q -f name=^/${NAME}$)" ]; then
  docker stop "$NAME" || true
fi

if [ "$(docker ps -aq -f name=^/${NAME}$)" ]; then
  docker rm "$NAME" || true
fi

# Try to disconnect from the network (ignore errors)
if docker network inspect "$NETWORK" >/dev/null 2>&1; then
  docker network disconnect "$NETWORK" "$NAME" >/dev/null 2>&1 || true
fi

if docker image inspect "$IMAGE" >/dev/null 2>&1; then
  docker rmi -f "$IMAGE"
  echo "Removed image $IMAGE"
else
  echo "Image $IMAGE not found"
fi

# Remove the network if it's now unused
if docker network inspect "$NETWORK" >/dev/null 2>&1; then
  CONTAINERS=$(docker network inspect "$NETWORK" --format '{{range $k,$v := .Containers}}{{$k}} {{end}}')
  if [ -z "$CONTAINERS" ]; then
    docker network rm "$NETWORK" >/dev/null 2>&1 || true
    echo "Removed network $NETWORK"
  fi
fi

echo "Removed container $NAME (if existed) and image $IMAGE"
