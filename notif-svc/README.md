# FitFlow `notif-svc`

Standalone Java/Spring Boot implementation of the FitFlow notification microservice for **Task 1 — Microservices + Docker**.

## Scope

This project implements the minimum Task 1 responsibilities for `notif-svc`:

- Create/send a notification. For now, delivery is represented by an application log.
- Persist and query a user's notification history.
- `GET /healthz` liveness endpoint.
- `GET /readyz` readiness endpoint that verifies PostgreSQL connectivity.
- Runs on port `8002`.
- Uses its own PostgreSQL instance and an application-specific DB account.
- Uses environment variables for passwords; `.env` is ignored by Git.
- Uses logical Docker hostnames (`notif-db`, `notif-svc`) rather than container IPs.

Task 2 (Consul), Task 3 (resilience/correlation IDs), Task 4 (JWT), and Task 5 (A2A) are intentionally outside this initial implementation.

## Project structure

```text
notif-svc/
├── notif-api/       # REST controllers, DTOs, Spring Boot entry point
├── notif-service/   # business/application logic
├── notif-dao/       # JPA entity + repository
├── docker/          # PostgreSQL initialization
├── Dockerfile
├── docker-compose.yml
└── pom.xml          # Maven multi-module parent
```

Dependency direction:

```text
notif-api -> notif-service -> notif-dao -> PostgreSQL
```

## Technology

- Java 25
- Spring Boot 4.0.8
- Maven multi-module build
- Spring MVC
- Spring Data JPA
- PostgreSQL 16
- Docker / Docker Compose

## Database and least privilege

The database container is initialized with two users:

1. `NOTIF_DB_ADMIN_USER`: bootstrap account used by PostgreSQL initialization.
2. `NOTIF_DB_USER`: application account used by `notif-svc`.

The Java service receives only the application credentials. The application account is granted `CONNECT`, schema `USAGE`, and DML permissions on the `notifications` table. It is not the PostgreSQL bootstrap/superuser account.

## Run with Docker Compose

Create the local environment file:

```bash
cp .env.example .env
```

Edit `.env` and replace both placeholder passwords.

Start the service and database:

```bash
docker compose up --build
```

The service is available at:

```text
http://localhost:8002
```

Inside the Docker network, other services should address it as:

```text
http://notif-svc:8002
```

## Endpoints

### Liveness

```bash
curl http://localhost:8002/healthz
```

Expected:

```json
{"status":"ok"}
```

### Readiness

```bash
curl http://localhost:8002/readyz
```

Expected while PostgreSQL is reachable:

```json
{"status":"ok"}
```

If PostgreSQL cannot be reached, the endpoint returns HTTP `503` with:

```json
{"status":"error"}
```

### Create/send notification

```bash
curl -i -X POST http://localhost:8002/notifications \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "user-123",
    "type": "BOOKING_CREATED",
    "message": "Your fitness class reservation was created."
  }'
```

Expected HTTP status: `201 Created`.

Example response:

```json
{
  "id": "7e85ec21-769a-43ff-b61d-601969a6ff21",
  "userId": "user-123",
  "type": "BOOKING_CREATED",
  "message": "Your fitness class reservation was created.",
  "status": "SENT",
  "createdAt": "2026-08-22T23:30:00Z"
}
```

The service also writes a log representing the notification send operation.

### Notification history by user

```bash
curl http://localhost:8002/notifications/users/user-123
```

Notifications are returned newest first.

## Build locally with Maven

Java 25 and Maven are required:

```bash
mvn clean package
```

Run the API module locally only after providing database environment variables:

```bash
export NOTIF_DB_HOST=localhost
export NOTIF_DB_PORT=5432
export NOTIF_DB_NAME=fitflow_notif
export NOTIF_DB_USER=notif_app
export NOTIF_DB_PASSWORD='<your-password>'

mvn -pl notif-api -am spring-boot:run
```

For the course deliverable, Docker Compose is the simplest way to run both `notif-svc` and its PostgreSQL database together.

## Useful Docker commands

```bash
# Start/rebuild
docker compose up --build

# Follow notif-svc logs
docker compose logs -f notif-svc

# Stop containers
docker compose down

# Stop and delete the local database volume
docker compose down -v
```

> The PostgreSQL initialization scripts run only when the database volume is created for the first time. If you change the initialization script during development, use `docker compose down -v` before starting again.
