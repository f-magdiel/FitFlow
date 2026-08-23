#!/usr/bin/env sh
set -e

NAME=${NAME:-notif-svc}
NETWORK=${NETWORK:-fitflow-net}

RUNNING_ID=$(docker ps -q -f name=^/${NAME}$)
ANY_ID=$(docker ps -aq -f name=^/${NAME}$)

if [ -n "$RUNNING_ID" ]; then
  docker stop "$NAME"
  docker rm "$NAME"
  echo "Stopped and removed running container $NAME"
elif [ -n "$ANY_ID" ]; then
  docker rm "$NAME" || true
  echo "Removed stopped container $NAME"
else
  echo "$NAME is not running and no stopped container exists"
fi

# Always attempt to disconnect from the network if it exists
if docker network inspect "$NETWORK" >/dev/null 2>&1; then
  docker network disconnect "$NETWORK" "$NAME" >/dev/null 2>&1 || true
fi
