# notif-svc (pendiente)

Responsable: _(asignar integrante del equipo)_
Puerto: **8002**

Este microservicio todavia no esta implementado. Este README documenta lo que debe exponer segun el
enunciado de Task 1 (ver `../Fitflow.md`) y las convenciones que sigue el resto del repo (ver `users-svc/`
como referencia de un servicio ya completo).

## Qué debe exponer (Task 1)

- Crear/enviar una notificacion (puede ser solo un log por ahora)
- Consultar historial de notificaciones de un usuario
- `GET /healthz` -> `{"status": "ok"}`
- `GET /readyz` -> `{"status": "ok"}` solo si la conexion a su base de datos funciona

## Convenciones del proyecto (obligatorias)

- Base de datos propia (`notif-db`, Postgres) con su propio usuario — dueño exclusivo de sus datos.
- Nada de passwords/secretos en el codigo: todo via variables de entorno (ver `.env.example` en la raiz).
- Su propio `Dockerfile` (multi-stage, igual que `users-svc/Dockerfile`).
- Agregar el servicio y su base de datos al `docker-compose.yml` de la raiz (hay un bloque comentado
  como guia).
- Comunicacion entre servicios por nombre logico (`http://notif-svc:8002`), nunca por IP.

## Sugerencia de stack

Si el equipo sigue con Java/Spring Boot, se puede generar el esqueleto igual que `users-svc`:

```bash
curl -G https://start.spring.io/starter.zip \
  -d type=maven-project -d language=java -d bootVersion=4.1.1 \
  -d baseDir=notif-svc -d groupId=com.fitflow -d artifactId=notif-svc \
  -d name=notif-svc -d packageName=com.fitflow.notif -d packaging=jar \
  -d javaVersion=21 -d dependencies=web,data-jpa,postgresql,validation,lombok \
  -o notif-svc.zip
```

(el enunciado permite tambien Python/FastAPI, Node/Express o Go si el equipo lo prefiere).
