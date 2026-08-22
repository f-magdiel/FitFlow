# users-svc

Responsable: Magdiel (yo)
Puerto: **8003**

La implementación de este microservicio se está desarrollando en la rama `feature/users-svc` y se
integrará a `develop` vía Pull Request. Este README solo marca la carpeta como parte de la
estructura base del proyecto.

## Qué debe exponer (Task 1)

- `POST` registrar un usuario nuevo
- `POST` login (devuelve un token JWT)
- `GET` obtener perfil de un usuario por ID
- `GET /healthz` -> `{"status": "ok"}`
- `GET /readyz` -> `{"status": "ok"}` solo si la conexión a su base de datos funciona

## Convenciones del proyecto (obligatorias)

- Base de datos propia (`users-db`, Postgres) con su propio usuario.
- Nada de passwords/secretos en el código: todo vía variables de entorno (ver `.env.example` en la
  raíz).
- Su propio `Dockerfile` (multi-stage).
- Se registra en el `docker-compose.yml` de la raíz con su propia base de datos.
- Comunicación entre servicios por nombre lógico (`http://users-svc:8003`), nunca por IP.

## Stack

Java 21 + Spring Boot (Maven), JWT con `jjwt`, BCrypt para hash de passwords, PostgreSQL.
