# booking-svc

Responsable: Sergio Garcia
Puerto: **8001**

Microservicio encargado de gestionar clases fitness y reservas dentro de FitFlow. Es dueño de
`booking-db` y no consulta directamente las bases de datos de `users-svc` ni `notif-svc`.

## Responsabilidades

- Listar clases disponibles.
- Crear reservas para usuarios existentes.
- Consultar reservas por ID.
- Listar reservas de un usuario.
- Cancelar reservas y liberar cupo.
- Validar usuarios llamando a `users-svc`.
- Enviar notificaciones llamando a `notif-svc`.
- Exponer `/healthz` y `/readyz`.

## Endpoints

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `GET` | `/api/classes/available` | Lista clases futuras con cupo disponible. |
| `POST` | `/api/bookings` | Crea una reserva. |
| `GET` | `/api/bookings/{id}` | Consulta una reserva por ID. |
| `GET` | `/api/bookings/users/{userId}` | Lista reservas de un usuario. |
| `DELETE` | `/api/bookings/{id}` | Cancela una reserva. |
| `GET` | `/healthz` | Verifica que el proceso responde. |
| `GET` | `/readyz` | Verifica conexion a PostgreSQL. |

### Crear Reserva

```http
POST /api/bookings
Content-Type: application/json
```

```json
{
  "userId": "00000000-0000-0000-0000-000000000000",
  "classId": "00000000-0000-0000-0000-000000000000"
}
```

`booking-svc` valida el usuario con:

```txt
GET http://users-svc:8003/api/users/{userId}
```

Si la reserva se confirma o cancela, intenta notificar con:

```txt
POST http://notif-svc:8002/notifications
```

El fallo de `notif-svc` no cancela la reserva; por ahora se registra en logs. En una fase posterior
se agregara resiliencia formal con timeout, retries, circuit breaker u outbox.

## Variables De Entorno

| Variable | Ejemplo |
| --- | --- |
| `SERVER_PORT` | `8001` |
| `DB_HOST` | `booking-db` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `booking_db` |
| `DB_USER` | `booking_user` |
| `DB_PASSWORD` | `change-me-booking-db-password` |
| `USERS_SVC_URL` | `http://users-svc:8003` |
| `NOTIF_SVC_URL` | `http://notif-svc:8002` |

## Ejecucion Con Docker Compose

Desde la raiz del repo:

```bash
cp .env.example .env
docker compose up --build booking-db booking-svc
```

Para levantar el sistema completo:

```bash
docker compose up --build
```

## Stack

Java 21 + Spring Boot, Maven, Spring Web MVC, Spring Data JPA, Bean Validation y PostgreSQL.
