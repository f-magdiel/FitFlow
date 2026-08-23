# FitFlow `notif-svc`

Lightweight Java/Spring Boot notification microservice. This README reflects the current repository layout, Docker usage, environment variables, and helper scripts present in the `notif-svc` folder.

## Overview

This module is implemented as a multi-module Maven project and is split to separate API, service, and persistence concerns. A runnable module named `notif-server` contains the Spring Boot `main()` to produce the executable JAR used by Docker.

Key runtime notes:
- The root `docker-compose.yml` orchestrates the database (`notif-db`) and the service (`notif-svc`).
- The Postgres host is exposed on the host as `55432:5432` to avoid conflicts with local Postgres instances.
- Default DB environment variables are injected and also set in `notif-api`'s `application.yml` so local runs work without extra env setup.
- Hibernate is configured with `ddl-auto: validate` — the schema must exist before the Spring app starts.

## Project structure (actual)

```text
notif-svc/
├── notif-api/       # Controllers, DTOs, shared Spring config (application.yml defaults)
├── notif-service/   # Business logic and service layer
├── notif-dao/       # JPA entities and repositories
├── notif-server/    # Runnable Spring Boot main; builds the executable jar
├── docker/          # PostgreSQL init scripts: docker/postgres/init
├── Dockerfile       # Builds a runtime image (copies notif-server jar)
├── start-notif-db.sh
├── stop-notif-db.sh
├── build-notif-app.sh
├── start-notif-app.sh
├── stop-notif-app.sh
├── remove-notif-app.sh
└── pom.xml          # Maven parent for modules above
```

Module dependency direction:

```text
notif-server -> notif-api -> notif-service -> notif-dao -> PostgreSQL
```

## Important files and locations

- `notif-svc/notif-api/src/main/resources/application.yml` — default datasource and server config used for both Docker and local runs (contains `${NOTIF_DB_*}` defaults).
- `notif-svc/docker/postgres/init/01-create-notif-schema.sh` — idempotent SQL used to create schema and roles; placed under Postgres init dir so the image bootstraps on first volume creation.
- `docker-compose.yml` (repo root) — use this to run both `notif-db` and `notif-svc` together.

## Environment variables (used names)

Set these for production/local parity. Defaults are provided in `notif-api`'s `application.yml` so a developer can run locally without extra env vars, but Docker compose will supply the intended values when used.

- `NOTIF_DB_NAME` (default `notifdb`)
- `NOTIF_DB_USER` (default `notif_user`)
- `NOTIF_DB_PASSWORD` (default `notif_pass`)
- `NOTIF_DB_HOST` (compose: `notif-db`, local default: `localhost`)
- `NOTIF_DB_PORT` (compose: `5432`, host-mapped default: `55432`)

Note: `spring.datasource.url` in `application.yml` resolves via `${NOTIF_DB_HOST:localhost}:${NOTIF_DB_PORT:55432}/${NOTIF_DB_NAME:notifdb}`.

## Docker / Compose

Preferred developer path (from repo root):

```bash
# build and start DB + service via root compose
docker compose up -d --build notif-db notif-svc

# follow logs
docker compose logs -f notif-svc
```

Key runtime details:
- The DB container uses a named volume (persisted data). Postgres init scripts under `notif-svc/docker/postgres/init` are executed only on first-time initialization of that volume.
- Host port mapping uses `55432:5432` so local Postgres on `5432` is unaffected.
- Compose sets `NOTIF_DB_HOST=notif-db` and `NOTIF_DB_PORT=5432` for the `notif-svc` service so the app connects to the internal container address.

## Helper scripts (in `notif-svc/`)

- `start-notif-db.sh` — ensures `fitflow-net` exists and starts the DB container (waits for readiness and bootstraps schema if needed).
- `stop-notif-db.sh` — stops and optionally removes the DB container.
- `build-notif-app.sh` — builds the `notif-server` module and the Docker image (uses `mvn -pl notif-server -am clean package -DskipTests`).
- `start-notif-app.sh` — runs the app container and ensures it can resolve `notif-db` (attaches network/alias or uses `--add-host` fallback).
- `stop-notif-app.sh` — stops and removes the running app container; supports `--remove-stopped`.
- `remove-notif-app.sh` — removes the app image and network if unused.

Use these scripts from the `notif-svc` folder. Example:

```bash
cd notif-svc
./build-notif-app.sh
./start-notif-app.sh
```

## Run locally (developer)

To run locally without Docker, ensure the DB is reachable and the environment variables are set. Defaults exist in `notif-api` so you can point to the host-mapped Postgres port if you started the DB via Docker Compose:

```bash
export NOTIF_DB_HOST=localhost
export NOTIF_DB_PORT=55432
export NOTIF_DB_NAME=notifdb
export NOTIF_DB_USER=notif_user
export NOTIF_DB_PASSWORD=notif_pass

# build and run only the server module
mvn -pl notif-server -am clean package
java -jar notif-server/target/*-jar-with-dependencies.jar
```

Or use Spring Boot run for quick dev iterations:

```bash
mvn -pl notif-api -am spring-boot:run
```

## Schema and initialization

- The SQL script at `notif-svc/docker/postgres/init/01-create-notif-schema.sh` is idempotent — it checks for existing roles and tables before creating them.
- If you change the init script and want it to re-run, remove the Postgres volume and recreate the container:

```bash
docker compose down -v
docker compose up -d --build notif-db
```

## Troubleshooting

- If the app tries `localhost:55432` when started by Compose, ensure `NOTIF_DB_HOST` and `NOTIF_DB_PORT` are present in the `notif-svc` service environment in the root `docker-compose.yml` and that you restarted the compose stack after edits.
- Check logs:

```bash
docker compose logs -f notif-db --tail 200
docker compose logs -f notif-svc --tail 200
```

## References

- Docker init scripts: `notif-svc/docker/postgres/init`
- Application defaults: `notif-svc/notif-api/src/main/resources/application.yml`
- Runnable module (jar): `notif-svc/notif-server`

---

If you'd like, I can also add a short `Makefile` or top-level `README` snippet that documents the exact `docker compose` command sequence and the most common quick-fix commands.
